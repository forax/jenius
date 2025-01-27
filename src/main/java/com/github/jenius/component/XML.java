package com.github.jenius.component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
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

    private NodeBuilder delegatingNodeBuilder() {
      return new SaxNodeAdapter(impl, actionStack) {
        @Override
        public NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
          impl.startElement("", name, name, AttributesUtil.asAttributes(map));
          children.accept(this);
          impl.endElement("", name, name);
          return this;
        }
      };
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
    public final NodeBuilder include(Reader reader) {
      Objects.requireNonNull(reader);
      XML.include(impl, reader);
      return this;
    }

    @Override
    public final NodeBuilder include(Node node) {
      Objects.requireNonNull(node);
      try {
        node.visit(impl);
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
      return this;
    }

    @Override
    public final void collect(BiConsumer<? super Node, ? super NodeBuilder> consumer) {
      Objects.requireNonNull(consumer);
      Objects.requireNonNull(consumer);
      var ignore = (Action.Ignore) actionStack.pop();
      var document = Node.createDocument();
      var node = document.createNode(ignore.name, AttributesUtil.asMap(ignore.attrs));
      actionStack.push(new Action.Collect(document, node, n -> consumer.accept(n, delegatingNodeBuilder())));
    }

    @Override
    public final NodeBuilder fragment(Consumer<? super NodeBuilder> children) {
      Objects.requireNonNull(children);
      children.accept(delegatingNodeBuilder());
      return this;
    }

    @Override
    public final void hide() {
      var _ = (Action.Ignore) actionStack.pop();
      actionStack.push(Action.EnumAction.HIDE);
    }

    abstract NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException;
  }

  private sealed interface Action {
    enum EnumAction implements Action { EMIT, HIDE }
    record Ignore(String name, Attributes attrs) implements Action {}
    record Replace(String newName) implements Action {}
    record Collect(Node document, Node node, Consumer<Node> consumer) implements Action {}
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
              throw new IllegalStateException("this builder has already called once " + name + " " + map);
            }
            calledOnce = true;
            var contentHandler = getContentHandler();
            contentHandler.startElement("", name, name, AttributesUtil.asAttributes(map));
            var _ = (Action.Ignore) actionsStack.pop();
            actionsStack.push(new Action.Replace(name));
            children.accept(super.delegatingNodeBuilder());
            return this;
          }
        };
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
        switch (actionsStack.peek()) {
          case null -> {}  // no action yet
          case Action.EnumAction.EMIT -> {}
          case Action.EnumAction.HIDE -> {
            actionsStack.push(Action.EnumAction.HIDE);
            return;
          }
          case Action.Ignore _, Action.Replace _ -> {}
          case Action.Collect(Node document, Node node, _) -> {
            var newNode = document.createNode(localName, AttributesUtil.asMap(attrs));
            node.appendChild(newNode);
            actionsStack.push(new Action.Collect(document, newNode, null));
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
          case Action.EnumAction.HIDE -> {}
          case Action.Ignore _ -> {}
          case Action.Replace(String newName) -> super.endElement("", newName, newName);
          case Action.Collect(_, Node node, Consumer<Node> consumer) -> {
            if (consumer != null) {
              consumer.accept(node);
            }
          }
        }
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        var action = Objects.requireNonNull(actionsStack.peek());
        switch (action) {
          case Action.EnumAction.EMIT -> super.characters(ch, start, length);
          case Action.EnumAction.HIDE -> {}
          case Action.Ignore _ -> {}
          case Action.Replace _ -> super.characters(ch, start, length);
          case Action.Collect(_, Node node, _) -> node.appendText(new String(ch, start, length));
        }
      }

      @Override
      public void ignorableWhitespace(char[] ch, int start, int length) {
        // just ignore them
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

  private static XMLReader createXMLReader(Node node) {
    return new XMLReader() {
      private ContentHandler contentHandler;
      private EntityResolver entityResolver;
      private DTDHandler dtdHandler;
      private ErrorHandler errorHandler;

      @Override
      public void setContentHandler(ContentHandler handler) {
        this.contentHandler = handler;
      }
      @Override
      public ContentHandler getContentHandler() {
        return contentHandler;
      }
      @Override
      public void setEntityResolver(EntityResolver resolver) {
        this.entityResolver = resolver;
      }
      @Override
      public EntityResolver getEntityResolver() {
        return entityResolver;
      }
      @Override
      public void setDTDHandler(DTDHandler handler) {
        this.dtdHandler = handler;
      }
      @Override
      public DTDHandler getDTDHandler() {
        return dtdHandler;
      }
      @Override
      public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
      }
      @Override
      public ErrorHandler getErrorHandler() {
        return errorHandler;
      }

      @Override
      public void parse(InputSource input) throws SAXException {
        contentHandler.declaration(null, null, null);
        node.visit(contentHandler);
      }

      @Override
      public void parse(String systemId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean getFeature(String name) {
        return false;
      }
      @Override
      public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
         throw new SAXNotRecognizedException();
      }
      @Override
      public Object getProperty(String name) {
        return null;
      }
      @Override
      public void setProperty(String name, Object value) throws SAXNotRecognizedException {
        throw new SAXNotRecognizedException();
      }
    };
  }

  public enum OutputKind {
    HTML, XML;

    private String methodName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  private static void transform(XMLReader xmlReader, InputSource inputSource, Result result, OutputKind outputKind) throws IOException {
    var transformerFactory = SAXTransformerFactory.newInstance();
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    try {
      transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch(TransformerConfigurationException e) {
      throw new IOException(e);
    }
    try {
      var transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, outputKind.methodName());
      if (outputKind == OutputKind.HTML) {
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "");
        xmlReader = HTMLElementValidator.validateHTMLElements(xmlReader);
      }
      var source = new SAXSource(xmlReader, inputSource);
      transformer.transform(source, result);
    } catch (TransformerException e) {
      throw new IOException(e);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  public static Node transform(Reader reader) throws IOException {
    Objects.requireNonNull(reader);
    var xmlReader = createXMLReader();
    var result = new DOMResult();
    transform(xmlReader, new InputSource(reader), result, OutputKind.XML);
    return new Node(result.getNode());
  }

  public static Node transform(Reader reader, ComponentStyle style) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(style);
    var xmlReader = createXMLReader();
    var result = new DOMResult();
    transform(filter(xmlReader, style), new InputSource(reader), result, OutputKind.XML);
    return new Node(result.getNode());
  }

  public static void transform(Reader reader, Writer writer, OutputKind outputKind, ComponentStyle style) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(writer);
    Objects.requireNonNull(outputKind);
    Objects.requireNonNull(style);
    var xmlReader = createXMLReader();
    var result = new StreamResult(writer);
    transform(filter(xmlReader, style), new InputSource(reader), result, outputKind);
  }

  public static Node transform(Node document, ComponentStyle style) {
    Objects.requireNonNull(document);
    Objects.requireNonNull(style);
    var xmlReader = createXMLReader(document);
    var result = new DOMResult();
    try {
      transform(filter(xmlReader, style), null, result, OutputKind.XML);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return new Node(result.getNode());
  }

  public static void transform(Node document, Writer writer, OutputKind outputKind, ComponentStyle style) throws IOException {
    Objects.requireNonNull(document);
    Objects.requireNonNull(writer);
    Objects.requireNonNull(outputKind);
    Objects.requireNonNull(style);
    var xmlReader = createXMLReader(document);
    var result = new StreamResult(writer);
    transform(filter(xmlReader, style), null, result, outputKind);
  }
}
