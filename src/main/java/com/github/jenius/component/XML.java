package com.github.jenius.component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jsoup.parser.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

public class XML {
  private static final class UncheckedSAXException extends RuntimeException {
    private final SAXException saxException;

    private UncheckedSAXException(SAXException saxException) {
      this.saxException = saxException;
    }
  }

  private static abstract class FilterHandler implements ContentHandler {
    private final ContentHandler delegate;

    private FilterHandler(ContentHandler delegate) {
      this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public final void setDocumentLocator(Locator locator) {
      throw new UnsupportedOperationException();
    }
    @Override
    public final void startPrefixMapping(String prefix, String uri) {
      throw new UnsupportedOperationException();
    }
    @Override
    public final void endPrefixMapping(String prefix) {
      throw new UnsupportedOperationException();
    }
    @Override
    public final void ignorableWhitespace(char[] ch, int start, int length) {
      throw new UnsupportedOperationException();
    }
    @Override
    public void processingInstruction(String target, String data) {
      throw new UnsupportedOperationException();
    }
    @Override
    public void skippedEntity(String name) {
      throw new UnsupportedOperationException();
    }
  }

  private static abstract class AbstractNodeBuilder implements NodeBuilder {
    private final FilterHandler filter;
    private final ArrayDeque<Action> actionStack;

    private AbstractNodeBuilder(FilterHandler filter, ArrayDeque<Action> actionStack) {
      this.filter = filter;
      this.actionStack = actionStack;
    }

    private NodeBuilder delegatingNodeBuilder() {
      return new AbstractNodeBuilder(filter, actionStack) {
        @Override
        public NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
          filter.startElement("", name, name, AttributesUtil.asAttributes(map));
          children.accept(this);
          filter.endElement("", name, name);
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
      try {
        filter.delegate.characters(text.toCharArray(), 0, text.length());
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
      return this;
    }

    @Override
    public final NodeBuilder include(Reader reader) {
      Objects.requireNonNull(reader);
      XML.include(filter, reader);
      return this;
    }

    @Override
    public final NodeBuilder include(Node node) {
      Objects.requireNonNull(node);
      try {
        node.visit(filter);
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

  private static ContentHandler filter(ContentHandler delegate, ComponentStyle style) {
    var actionsStack = new ArrayDeque<Action>();
    return new FilterHandler(delegate) {
      private NodeBuilder rewritingNodeBuilder() {
        return new AbstractNodeBuilder(this, actionsStack) {
          private boolean calledOnce;

          @Override
          public NodeBuilder saxNode(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
            if (calledOnce) {
              throw new IllegalStateException("this builder has already called once " + name + " " + map);
            }
            calledOnce = true;
            delegate.startElement("", name, name, AttributesUtil.asAttributes(map));
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
          delegate.startElement(uri, localName, qName, attrs);
        }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
        var action = actionsStack.pop();
        switch (action) {
          case Action.EnumAction.EMIT -> delegate.endElement(uri, localName, qName);
          case Action.EnumAction.HIDE -> {}
          case Action.Ignore _ -> {}
          case Action.Replace(String newName) -> delegate.endElement("", newName, newName);
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
          case Action.EnumAction.EMIT -> delegate.characters(ch, start, length);
          case Action.EnumAction.HIDE -> {}
          case Action.Ignore _ -> {}
          case Action.Replace _ -> delegate.characters(ch, start, length);
          case Action.Collect(_, Node node, _) -> node.appendText(new String(ch, start, length));
        }
      }

      @Override
      public void startDocument() throws SAXException {
        delegate.startDocument();
      }

      @Override
      public void endDocument() throws SAXException {
        delegate.endDocument();
      }
    };
  }

  private static void include(FilterHandler filter, Reader reader) {
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

  public enum OutputKind {
    HTML, XML
  }

  private static org.jsoup.nodes.Document rewrite(org.jsoup.nodes.Document document, ComponentStyle style) throws IOException {
    var result = new org.jsoup.nodes.Document("");
    try {
      var filter = filter(Node.asContentHandler(result), style);
      Node.visit(document, filter);
    } catch (SAXException e) {
      throw new IOException(e);
    }
    return result;
  }

  private static org.jsoup.nodes.Document parseXML(Reader reader) throws IOException {
    var parser = Parser.xmlParser();
    try {
      return parser.parseInput(reader, "");
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  public static Node transform(Reader reader) throws IOException {
    Objects.requireNonNull(reader);
    return new Node(parseXML(reader));
  }

  public static Node transform(Reader reader, ComponentStyle style) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(style);
    return new Node(rewrite(parseXML(reader), style));
  }

  public static void transform(Reader reader, Writer writer, OutputKind outputKind, ComponentStyle style) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(writer);
    Objects.requireNonNull(outputKind);
    Objects.requireNonNull(style);
    var result = rewrite(parseXML(reader), style);
    result.outputSettings().indentAmount(2);
    result.outputSettings().syntax(switch (outputKind) {
      case XML -> org.jsoup.nodes.Document.OutputSettings.Syntax.xml;
      case HTML -> org.jsoup.nodes.Document.OutputSettings.Syntax.html;
    });
    writer.write(result.outerHtml());
  }

  public static Node transform(Node document, ComponentStyle style) {
    Objects.requireNonNull(document);
    Objects.requireNonNull(style);
    try {
      return new Node(rewrite((org.jsoup.nodes.Document) document.jsoupNode, style));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static void transform(Node document, Writer writer, OutputKind outputKind, ComponentStyle style) throws IOException {
    Objects.requireNonNull(document);
    Objects.requireNonNull(writer);
    Objects.requireNonNull(outputKind);
    Objects.requireNonNull(style);
    var result = rewrite((org.jsoup.nodes.Document) document.jsoupNode, style);
    result.outputSettings().indentAmount(2);
    result.outputSettings().syntax(switch (outputKind) {
      case XML -> org.jsoup.nodes.Document.OutputSettings.Syntax.xml;
      case HTML -> org.jsoup.nodes.Document.OutputSettings.Syntax.html;
    });
    writer.write(result.outerHtml());
  }
}
