package com.github.jenius.component;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Node {
  private static final class AttributeMap extends AbstractMap<String, String> {
    private final org.jsoup.nodes.Attributes attributes;
    private final int size;

    private AttributeMap(org.jsoup.nodes.Attributes attributes) {
      this.attributes = attributes;
      // jsoup bug: need to use asList().size() because size() count internal attributes too;
      this.size = attributes.isEmpty() ? 0 : attributes.asList().size();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
      return new AbstractSet<>() {
        @Override
        public int size() {
          return size;
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
          var it = attributes.iterator();
          return new Iterator<>() {

            @Override
            public boolean hasNext() {
              return it.hasNext();
            }

            @Override
            public Entry<String, String> next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              var attr = it.next();
              return Map.entry(attr.getKey(), attr.getValue());
            }
          };
        }
      };
    }

    @Override
    public boolean containsKey(Object key) {
      return getOrDefault(key, null) != null;
    }

    @Override
    public String get(Object key) {
      return getOrDefault(key, null);
    }

    @Override
    public String getOrDefault(Object key, String defaultValue) {
      Objects.requireNonNull(key);
      if (!(key instanceof String name)) {
        return defaultValue;
      }
      if (!attributes.hasKey(name)) {  // let's hope indexOf will be inlined
        return defaultValue;
      }
      return attributes.get(name);
    }
  }

  final org.jsoup.nodes.Node jsoupNode;

  Node(org.jsoup.nodes.Node jsoupNode) {
    this.jsoupNode = jsoupNode;
  }

  public static Node createDocument() {
    var document = new org.jsoup.nodes.Document("");
    return new Node(document);
  }

  public Node createNode(String name) {
    return createNode(name, Map.of());
  }

  public Node createNode(String name, List<Node> childNodes) {
    return createNode(name, Map.of(), childNodes);
  }

  public Node createNode(String name, Map<String, String> attributes) {
    return createNode(name, attributes, List.of());
  }

  public Node createNode(String name, Map<String, String> attributes, List<Node> childNodes) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(attributes);
    Objects.requireNonNull(childNodes);
    var element = new org.jsoup.nodes.Element(name);
    attributes.forEach(element::attr);
    List.copyOf(childNodes).forEach(n -> element.appendChild(n.jsoupNode));
    return new Node(element);
  }

  public void appendChild(Node node) {
    Objects.requireNonNull(node);
    ((org.jsoup.nodes.Element) jsoupNode).appendChild(node.jsoupNode);
  }

  public void appendText(String content) {
    Objects.requireNonNull(content);
    ((org.jsoup.nodes.Element) jsoupNode).appendText(content);
  }

  public Optional<Node> getFirstElement() {
    return jsoupNode.childNodes().stream()
        .filter(n -> n instanceof org.jsoup.nodes.Element)
        .findFirst()
        .map(Node::new);
  }

  private static void toDebugString(org.jsoup.nodes.Node jsoupNode, StringBuilder builder, String indent) {
    builder.append(indent).append("Node: ").append(jsoupNode.nodeName())
        .append(" [Type: ").append(jsoupNode.getClass().getSimpleName()).append("]");
    for(var attr : jsoupNode.attributes()) {
      builder.append(indent).append("  Attribute: ")
          .append(attr.getKey()).append(" = ").append(attr.getValue()).append('\n');
    }
    for (var child : jsoupNode.childNodes()) {
      toDebugString(child, builder, indent + "  ");
    }
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();
    toDebugString(jsoupNode, builder, "  ");
    return builder.toString();
  }

  public String name() {
    return jsoupNode.nodeName();
  }

  public String text() {
    return jsoupNode instanceof org.jsoup.nodes.Element element ? element.text() : "";
  }

  static void visit(org.jsoup.nodes.Node jsoupNode, XML.ContentHandler handler) {
    switch (jsoupNode) {
      case org.jsoup.nodes.XmlDeclaration _ -> {
          handler.declaration(jsoupNode.attr("version"), jsoupNode.attr("encoding"));
          return;
      }
      case org.jsoup.nodes.DocumentType _ -> { return; }  // ignore
      case org.jsoup.nodes.TextNode textNode -> {
        if (textNode.parent() instanceof org.jsoup.nodes.Document) {
          return;
        }
        var text = textNode.getWholeText();
        handler.characters(text);
        return;
      }
      case org.jsoup.nodes.Document _ -> handler.startDocument();
      case org.jsoup.nodes.Element element -> {
        var name = element.nodeName();
        handler.startElement(name, new AttributeMap(element.attributes()));
      }
      default -> {}
    }
    for(var i = 0; i < jsoupNode.childNodeSize(); i++) {
      visit(jsoupNode.childNode(i), handler);
    }
    switch (jsoupNode) {
      case org.jsoup.nodes.Document _ -> handler.endDocument();
      case org.jsoup.nodes.Element element -> {
        var name = element.nodeName();
        handler.endElement(name);
      }
      default -> {}
    }
  }

  void visit(XML.ContentHandler handler) {
    visit(jsoupNode, handler);
  }

  static XML.ContentHandler asContentHandler(org.jsoup.nodes.Document document) {
    var stack = new ArrayDeque<org.jsoup.nodes.Element>();
    stack.push(document);
    return new XML.ContentHandler() {
      @Override
      public void declaration(String version, String encoding) {
        var declaration = new org.jsoup.nodes.XmlDeclaration("xml", false)
            .attr("version", version)
            .attr("encoding", encoding);
        document.appendChild(declaration);
      }

      @Override
      public void startElement(String name, Map<String,String> attrs) {
        var element = document.createElement(name);
        if (attrs instanceof AttributeMap attributeMap) {
          element.attributes().addAll(attributeMap.attributes);
        } else {
          for (var entry : attrs.entrySet()) {
            element.attr(entry.getKey(), entry.getValue());
          }
        }
        var parent = Objects.requireNonNull(stack.peek());
        parent.appendChild(element);
        stack.push(element);
      }

      @Override
      public void endElement(String name) {
        assert stack.peek() != null && stack.peek().nodeName().equals(name);
        stack.pop();
      }

      @Override
      public void characters(String text) {
        var element = Objects.requireNonNull(stack.peek());
        element.appendText(text);
      }

      @Override
      public void startDocument() {}
      @Override
      public void endDocument() {}
    };
  }

  public Map<String, String> attributes() {
    var attributes = jsoupNode.attributes();
    return new AttributeMap(attributes);
  }

  public List<Node> elements() {
    return IntStream.range(0, jsoupNode.childNodeSize())
        .mapToObj(jsoupNode::childNode)
        .filter(n -> n instanceof org.jsoup.nodes.Element)
        .map(Node::new)
        .toList();
  }

  public List<Node> childNodes() {
    class NodeList extends AbstractList<Node> implements RandomAccess {
      @Override
      public int size() {
        return jsoupNode.childNodeSize();
      }

      @Override
      public Node get(int index) {
        Objects.checkIndex(index, size());
        return new Node(jsoupNode.childNode(index));
      }
    }
    return new NodeList();
  }

  private static org.jsoup.nodes.Element element(org.jsoup.nodes.Node jsoupNode, String name) {
    Objects.requireNonNull(name);
    return jsoupNode.childNodes().stream()
        .flatMap(n -> n instanceof org.jsoup.nodes.Element element ? Stream.of(element) : null)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no element named " + name + " found"));
  }

  public Node path(String... names) {
    Objects.requireNonNull(names);
    var domNode = this.jsoupNode;
    for(var name : names) {
      domNode = element(domNode, name);
    }
    return new Node(domNode);
  }

  public Optional<Node> find(String name) {
    Objects.requireNonNull(name);
    var stack = new ArrayDeque<org.jsoup.nodes.Node>();
    stack.offer(jsoupNode);
    org.jsoup.nodes.Node current;
    while((current = stack.poll()) != null) {
      if (name.equals(current.nodeName())) {
        return Optional.of(new Node(current));
      }
      for(var i = 0; i < current.childNodeSize(); i++) {
        var item = current.childNode(i);
        stack.push(item);
      }
    }
    return Optional.empty();
  }

  public void removeFromParent() {
    var parent = jsoupNode.parent();
    if (parent == null) {
      throw new IllegalStateException("no parent");
    }
    jsoupNode.remove();
  }
}
