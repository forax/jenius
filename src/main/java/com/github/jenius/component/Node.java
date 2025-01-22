package com.github.jenius.component;

import org.w3c.dom.Element;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;

public final class Node {
  private final org.w3c.dom.Node domNode;
  private String name;
  private String text;
  private Map<String, String> attributes;
  private List<Node> children;

  Node(org.w3c.dom.Node domNode) {
    this.domNode = domNode;
  }

  public Node(String name, Map<String, String> attributes) {
    this(null);
    this.name = Objects.requireNonNull(name);
    this.text = "";
    this.attributes = CompactMap.copyOf(attributes);
    this.children = new ArrayList<>();
  }

  public void text(String text) {
    if (domNode != null) {
      throw new UnsupportedOperationException("unmodifiable node");
    }
    this.text = text;
  }

  public void add(Node node) {
    Objects.requireNonNull(node);
    if (domNode != null) {
      throw new UnsupportedOperationException("unmodifiable node");
    }
    children.add(node);
  }

  public Node getFirst() {
    return children().getFirst();
  }

  private static void toDOMString(org.w3c.dom.Node domNode, StringBuilder builder, String indent) {
    var nodeValue = domNode.getNodeValue();
    builder.append(indent).append("Node: ")
        .append(domNode.getNodeName()).append(" [Type: ").append(domNode.getNodeType())
        .append(nodeValue != null ? ", Value: " + nodeValue : "").append("]\n");
    if (domNode instanceof Element) {
      var attributes = domNode.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
        var attribute = attributes.item(i);
        builder.append(indent).append("  Attribute: ")
            .append(attribute.getNodeName()).append(" = ").append(attribute.getNodeValue()).append('\n');
      }
    }
    var children = domNode.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      new Node(children.item(i)).toString(builder, indent + "  ");
    }
  }

  private void toNodeString(StringBuilder builder, String indent) {
    builder.append(indent).append("Node: ").append(name).append("]\n");
    attributes.forEach((name, value) -> {
      builder.append(indent).append("  Attribute: ").append(name).append(" = ").append(value).append('\n');
    });
    for (var node : children) {
      node.toString(builder, indent + "  ");
    }
  }

  private void toString(StringBuilder builder, String indent) {
    if (domNode == null) {
      toNodeString(builder, indent);
    }
    toDOMString(domNode, builder, indent);
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();
    toString(builder, "  ");
    return builder.toString();
  }

  public String text() {
    if (text != null) {
      return text;
    }
    return domNode.getFirstChild().getTextContent();
  }

  public Map<String, String> attributes() {
    if (attributes != null) {
      return attributes;
    }
    var domMap = domNode.getAttributes();
    return new AbstractMap<>() {
      @Override
      public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<>() {
          @Override
          public int size() {
            return domMap.getLength();
          }

          @Override
          public Iterator<Entry<String, String>> iterator() {
            return new Iterator<>() {
              private int index;

              @Override
              public boolean hasNext() {
                return index < size();
              }

              @Override
              public Entry<String, String> next() {
                if (!hasNext()) {
                  throw new NoSuchElementException();
                }
                var item = domMap.item(index++);
                return Map.entry(item.getNodeName(), item.getNodeValue());
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
        if (!(key instanceof String name)) {
          return defaultValue;
        }
        var item =  domMap.getNamedItem(name);
        return item.getNodeValue();
      }
    };
  }

  public List<Node> children() {
    if (children != null) {
      return children;
    }
    var domList = domNode.getChildNodes();
    class NodeList extends AbstractList<Node> implements RandomAccess {
      @Override
      public int size() {
        return domList.getLength();
      }

      @Override
      public Node get(int index) {
        Objects.checkIndex(index, size());
        return new Node(domList.item(index));
      }
    }
    return new NodeList();
  }
}
