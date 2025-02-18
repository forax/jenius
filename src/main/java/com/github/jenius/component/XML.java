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

public class XML {
  interface ContentHandler {
    void declaration(String version, String encoding);
    void startDocument();
    void endDocument();
    void startElement(String name, Map<String,String> attrs);
    void endElement(String name);
    void characters(String content);
  }

  private static abstract class FilterHandler implements ContentHandler {
    private final ContentHandler delegate;

    private FilterHandler(ContentHandler delegate) {
      this.delegate = Objects.requireNonNull(delegate);
    }
  }

  private static NodeBuilder delegatingNodeBuilder(FilterHandler filter, ArrayDeque<Action> actionStack) {
    return new AbstractNodeBuilder(filter, actionStack) {
      @Override
      public NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(map);
        Objects.requireNonNull(children);
        filter.startElement(name, map);
        children.accept(this);
        filter.endElement(name);
        return this;
      }
    };
  }

  private static abstract class AbstractNodeBuilder implements NodeBuilder {
    private final FilterHandler filter;
    private final ArrayDeque<Action> actionStack;

    private AbstractNodeBuilder(FilterHandler filter, ArrayDeque<Action> actionStack) {
      this.filter = filter;
      this.actionStack = actionStack;
    }

    @Override
    public final NodeBuilder text(String text) {
      Objects.requireNonNull(text);
      filter.delegate.characters(text);
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
      node.visit(filter);
      return this;
    }

    @Override
    public final void collect(BiConsumer<? super Node, ? super NodeBuilder> consumer) {
      Objects.requireNonNull(consumer);
      var ignore = (Action.Ignore) actionStack.pop();
      var document = Node.createDocument();
      var node = document.createNode(ignore.name, ignore.attrs);
      var action = new Action.Collect(document, node, n -> consumer.accept(n, delegatingNodeBuilder(filter, actionStack)));
      actionStack.push(action);
    }

    @Override
    public final NodeBuilder fragment(Consumer<? super NodeBuilder> children) {
      Objects.requireNonNull(children);
      children.accept(delegatingNodeBuilder(filter, actionStack));
      return this;
    }

    @Override
    public final void hide() {
      var _ = (Action.Ignore) actionStack.pop();
      actionStack.push(Action.EnumAction.HIDE);
    }

    @Override
    public final void around(Consumer<? super NodeBuilder> preBuilder, Consumer<? super NodeBuilder> postBuilder) {
      var action = actionStack.pop();
      var newName = switch (action) {
        case Action.Ignore _ -> null;
        case Action.Replace(String _newName) -> _newName;
        case Action.EnumAction _, Action.Collect _, Action.Around _ -> throw new IllegalStateException("can not use around here");
      };
      preBuilder.accept(delegatingNodeBuilder(filter, actionStack));
      actionStack.push(new Action.Around(postBuilder, newName));
    }
  }

  private sealed interface Action {
    enum EnumAction implements Action { EMIT, HIDE }
    record Ignore(String name, Map<String,String> attrs) implements Action {}
    record Replace(String newName) implements Action {}
    record Collect(Node document, Node node, Consumer<Node> consumer) implements Action {}
    record Around(Consumer<? super NodeBuilder> post, String newName) implements Action {}
  }

  private static ContentHandler filter(ContentHandler delegate, ComponentStyle style) {
    var actionsStack = new ArrayDeque<Action>();
    return new FilterHandler(delegate) {
      private NodeBuilder rewritingNodeBuilder() {
        return new AbstractNodeBuilder(this, actionsStack) {
          private boolean calledOnce;

          @Override
          public NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(map);
            Objects.requireNonNull(children);
            if (calledOnce) {
              throw new IllegalStateException("this builder has already called once " + name + " " + map);
            }
            calledOnce = true;
            delegate.startElement(name, map);
            var _ = (Action.Ignore) actionsStack.pop();
            actionsStack.push(new Action.Replace(name));
            children.accept(delegatingNodeBuilder(super.filter, actionsStack));
            return this;
          }
        };
      }

      @Override
      public void declaration(String version, String encoding) {
        delegate.declaration(version, encoding);
      }

      @Override
      public void startDocument() {
        delegate.startDocument();
      }

      @Override
      public void endDocument() {
        delegate.endDocument();
      }

      @Override
      public void startElement(String name, Map<String,String> attrs) {
        var action = actionsStack.peek();
        if (startElementAction(name, attrs, action) == ApplyStyleResult.DO_NOT_APPLY) {
          return;
        }

        var componentOpt = style.lookup(name);
        if (componentOpt.isPresent()) {
          var component = componentOpt.orElseThrow();
          actionsStack.push(new Action.Ignore(name, attrs));
          component.render(name, attrs, rewritingNodeBuilder());
        } else {
          actionsStack.push(Action.EnumAction.EMIT);
          delegate.startElement(name, attrs);
        }
      }

      @Override
      public void characters(String content) {
        var action = Objects.requireNonNull(actionsStack.peek());
        charactersAction(content, action);
      }

      @Override
      public void endElement(String name) {
        var action = actionsStack.pop();
        endElementAction(name, action);
      }

      private enum ApplyStyleResult { DO_APPLY, DO_NOT_APPLY }

      private ApplyStyleResult startElementAction(String name, Map<String, String> attrs, Action action) {
        return switch (action) {
          case null -> ApplyStyleResult.DO_APPLY;  // no action yet
          case Action.EnumAction.EMIT -> ApplyStyleResult.DO_APPLY;
          case Action.EnumAction.HIDE -> {
            actionsStack.push(Action.EnumAction.HIDE);
            yield ApplyStyleResult.DO_NOT_APPLY;
          }
          case Action.Ignore _, Action.Replace _, Action.Around _ -> ApplyStyleResult.DO_APPLY;
          case Action.Collect(Node document, Node node, _) -> {
            var newNode = document.createNode(name, attrs);
            node.appendChild(newNode);
            actionsStack.push(new Action.Collect(document, newNode, null));
            yield ApplyStyleResult.DO_NOT_APPLY;
          }
        };
      }

      private void charactersAction(String content, Action action) {
        switch (action) {
          case Action.EnumAction.EMIT -> delegate.characters(content);
          case Action.EnumAction.HIDE -> {}
          case Action.Ignore _ -> {}
          case Action.Replace _, Action.Around _ -> delegate.characters(content);
          case Action.Collect(_, Node node, _) -> node.appendText(content);
        }
      }

      private void endElementAction(String name, Action action) {
        switch (action) {
          case Action.EnumAction.EMIT -> delegate.endElement(name);
          case Action.EnumAction.HIDE -> {}
          case Action.Ignore _ -> {}
          case Action.Replace(String newName) -> delegate.endElement(newName);
          case Action.Collect(_, Node node, Consumer<Node> consumer) -> {
            if (consumer != null) {
              consumer.accept(node);
            }
          }
          case Action.Around(Consumer<? super NodeBuilder> postConsumer, String newName) -> {
            postConsumer.accept(delegatingNodeBuilder(this, actionsStack));
            if (newName != null) {
              delegate.endElement(newName);
            }
          }
        }
      }
    };
  }

  private static void include(FilterHandler filter, Reader reader) {
    org.jsoup.nodes.Document document;
    try {
      document = parseXML(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Node.visit(document, new ContentHandler() {
      @Override
      public void declaration(String version, String encoding) {}
      @Override
      public void startDocument() {}
      @Override
      public void endDocument() {}

      @Override
      public void startElement(String name, Map<String, String> attrs) {
        filter.startElement(name, attrs);
      }

      @Override
      public void endElement(String name) {
        filter.endElement(name);
      }

      @Override
      public void characters(String content) {
        filter.characters(content);
      }
    });
  }

  public enum OutputKind {
    HTML, XML
  }

  private static org.jsoup.nodes.Document rewrite(org.jsoup.nodes.Document document, ComponentStyle style) {
    var result = new org.jsoup.nodes.Document("");
    var filter = filter(Node.asContentHandler(result), style);
    Node.visit(document, filter);
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
    return new Node(rewrite((org.jsoup.nodes.Document) document.jsoupNode, style));
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
