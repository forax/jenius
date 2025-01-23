package com.github.jenius.component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

public class XML {
  private static final class UncheckedSAXException extends RuntimeException {
    private final SAXException saxException;

    private UncheckedSAXException(SAXException saxException) {
      this.saxException = saxException;
    }
  }

  private static abstract class SaxNodeAdapter implements NodeBuilder {
    private final XMLFilterImpl impl;
    private final ArrayDeque<Action> actionStack;

    private SaxNodeAdapter(XMLFilterImpl impl, ArrayDeque<Action> actionStack) {
      this.impl = impl;
      this.actionStack = actionStack;
    }

    @Override
    public final NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(map);
      Objects.requireNonNull(children);
      try {
        return saxNode(name, map, children);
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
    }

    @Override
    public final NodeBuilder text(String text) {
      Objects.requireNonNull(text);
      var contentHandler = impl.getContentHandler();
      try {
        contentHandler.characters(text.toCharArray(), 0, text.length());
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
      return this;
    }

    @Override
    public final void replay(UnaryOperator<Node> function) {
      var ignore = (Action.Ignore) actionStack.pop();
      var document = Node.createDocument();
      var node = document.createNode(ignore.name, AttributesUtil.asMap(ignore.attrs));
      actionStack.push(new Action.Replay(document, node, function));
    }

    @Override
    public final NodeBuilder include(Reader reader) {
      XML.include(impl, reader);
      return this;
    }

    abstract NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException;
  }

  private sealed interface Action {
    enum EnumAction implements Action { EMIT }
    record Ignore(String name, Attributes attrs) implements Action {}
    record Replace(String newName) implements Action {}
    record Replay(Node document, Node node, UnaryOperator<Node> function) implements Action {}
  }

  private static XMLFilterImpl filter(XMLReader xmlReader, ComponentStyle style) {
    var actionsStack = new ArrayDeque<Action>();
    return new XMLFilterImpl(xmlReader) {
      private NodeBuilder rewritingNodeBuilder() {
        return new SaxNodeAdapter(this, actionsStack) {
          private boolean calledOnce;

          @Override
          public NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
            if (calledOnce) {
              throw new IllegalStateException("this builder has already called once");
            }
            calledOnce = true;
            var contentHandler = getContentHandler();
            contentHandler.startElement("", name, name, AttributesUtil.asAttributes(map));
            var _ = (Action.Ignore) actionsStack.pop();
            actionsStack.push(new Action.Replace(name));
            children.accept(delegatingNodeBuilder());
            return this;
          }
        };
      }

      private NodeBuilder delegatingNodeBuilder() {
        return new SaxNodeAdapter(this, actionsStack) {
          @Override
          public NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
            startElement("", name, name, AttributesUtil.asAttributes(map));
            children.accept(this);
            endElement("", name, name);
            return this;
          }
        };
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
        switch (actionsStack.peek()) {
          case null -> {}  // no action yet
          case Action.EnumAction _, Action.Ignore _, Action.Replace _ -> {}
          case Action.Replay(Node document, Node node, _) -> {
            var newNode = document.createNode(localName, AttributesUtil.asMap(attrs));
            node.appendChild(newNode);
            actionsStack.push(new Action.Replay(document, newNode, null));
            return;
          }
        }
        var componentOpt = style.lookup(localName);
        if (componentOpt.isPresent()) {
          var component = componentOpt.orElseThrow();
          actionsStack.push(new Action.Ignore(localName, attrs));
          try {
            component.render(localName, AttributesUtil.asMap(attrs), rewritingNodeBuilder());
          } catch (UncheckedSAXException e) {
            throw e.saxException;
          }
        } else {
          actionsStack.push(Action.EnumAction.EMIT);
          super.startElement(uri, localName, qName, attrs);
        }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
        var action = actionsStack.pop();
        switch (action) {
          case Action.EnumAction.EMIT -> super.endElement(uri, localName, qName);
          case Action.Ignore _ -> {}
          case Action.Replace(String newName) -> super.endElement("", newName, newName);
          case Action.Replay(_, Node node, UnaryOperator<Node> function) -> {
            if (function != null) {
              var newNode = function.apply(node);
              newNode.visit(this);
            }
          }
        }
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        var action = Objects.requireNonNull(actionsStack.peek());
        switch (action) {
          case Action.EnumAction.EMIT -> super.characters(ch, start, length);
          case Action.Ignore _ -> {}
          case Action.Replace _ -> super.characters(ch, start, length);
          case Action.Replay(_, Node node, _) -> node.appendText(new String(ch, start, length));
        }
      }
    };
  }

  private static void include(XMLFilterImpl filter, Reader reader) {
    var parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    try {
      var parser = parserFactory.newSAXParser();
      parser.parse(new InputSource(reader), new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
          filter.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
          filter.endElement(uri, localName, qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
          filter.characters(ch, start, length);
        }
      });
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    } catch (SAXException e) {
      throw new UncheckedSAXException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void transform(XMLReader xmlReader, InputSource inputSource, Result result, ComponentStyle style) throws IOException {
    var transformerFactory = SAXTransformerFactory.newInstance();
    try {
      var transformer = transformerFactory.newTransformer();
      var source = new SAXSource(filter(xmlReader, style), inputSource);
      transformer.transform(source, result);
    } catch (TransformerException e) {
      throw new IOException(e);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private static XMLReader createXMLReader() throws IOException {
    var parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    try {
      var parser = parserFactory.newSAXParser();
      return parser.getXMLReader();
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    } catch (SAXException e) {
      throw new IOException(e);
    }
  }

  public static Node transform(Reader reader, ComponentStyle style) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(style);
    var xmlReader = createXMLReader();
    var result = new DOMResult();
    transform(xmlReader, new InputSource(reader), result, style);
    return new Node(result.getNode());
  }

  public static void transform(Reader reader, Writer writer, ComponentStyle style) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(style);
    Objects.requireNonNull(writer);
    var xmlReader = createXMLReader();
    var result = new StreamResult(writer);
    transform(xmlReader, new InputSource(reader), result, style);
  }
}
