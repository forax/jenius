package com.github.jenius.component;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;

public final class Node {
  private final org.w3c.dom.Node domNode;

  Node(org.w3c.dom.Node domNode) {
    this.domNode = domNode;
  }

  public static Node createDocument() {
    var factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new UncheckedIOException(new IOException(e));
    }
    var document = builder.newDocument();
    return new Node(document);
  }

  private org.w3c.dom.Document getDomDocument() {
    if (domNode instanceof org.w3c.dom.Document document) {
      return document;
    }
    return domNode.getOwnerDocument();
  }

  public Node createNode(String name) {
    return createNode(name, Map.of());
  }

  public Node createNode(String name, List<Node> children) {
    return createNode(name, Map.of(), children);
  }

  public Node createNode(String name, Map<String, String> attributes) {
    return createNode(name, attributes, List.of());
  }

  public Node createNode(String name, Map<String, String> attributes, List<Node> children) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(attributes);
    Objects.requireNonNull(children);
    var document = getDomDocument();
    var element = document.createElement(name);
    attributes.forEach(element::setAttribute);
    List.copyOf(children).forEach(n -> element.appendChild(n.domNode));
    return new Node(element);
  }

  public void appendChild(Node node) {
    Objects.requireNonNull(node);
    domNode.appendChild(node.domNode);
  }

  public void appendText(String content) {
    Objects.requireNonNull(content);
    var document = getDomDocument();
    var text = document.createTextNode(content);
    domNode.appendChild(text);
  }

  public Node getFirst() {
    return children().getFirst();
  }

  private static void toDOMString(org.w3c.dom.Node domNode, StringBuilder builder, String indent) {
    var nodeValue = domNode.getNodeValue();
    builder.append(indent).append("Node: ")
        .append(domNode.getNodeName()).append(" [Type: ").append(domNode.getNodeType())
        .append(nodeValue != null ? ", Value: " + nodeValue : "").append("]\n");
    var attributes = domNode.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        var attribute = attributes.item(i);
        builder.append(indent).append("  Attribute: ")
            .append(attribute.getNodeName()).append(" = ").append(attribute.getNodeValue()).append('\n');
      }
    }
    var children = domNode.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      toDOMString(children.item(i), builder, indent + "  ");
    }
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();
    toDOMString(domNode, builder, "  ");
    return builder.toString();
  }

  public String name() {
    return domNode.getNodeName();
  }

  public String text() {
    return domNode.getFirstChild().getTextContent();
  }

  private static void visit(org.w3c.dom.Node domNode, ContentHandler handler) throws SAXException {
    var name = domNode.getNodeName();
    if (domNode.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
      var text = domNode.getTextContent();
      handler.characters(text.toCharArray(), 0, text.length());
      return;
    }
    handler.startElement(null, name, name, AttributesUtil.asAttributes(domNode.getAttributes()));
    var domList = domNode.getChildNodes();
    for(var i = 0; i < domList.getLength(); i++) {
      visit(domList.item(i), handler);
    }
    handler.endElement("", name, name);
  }

  void visit(ContentHandler handler) throws SAXException {
    visit(domNode, handler);
  }

  public Map<String, String> attributes() {
    var domMap = domNode.getAttributes();
    return new AbstractMap<>() {
      @Override
      public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<>() {
          @Override
          public int size() {
            return domMap == null ? 0 : domMap.getLength();
          }

          @Override
          public Iterator<Entry<String, String>> iterator() {
            if (domMap == null) {
              return Collections.emptyIterator();
            }
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
        Objects.requireNonNull(key);
        if (!(key instanceof String name)) {
          return defaultValue;
        }
        var item =  domMap.getNamedItem(name);
        return item.getNodeValue();
      }
    };
  }

  public List<Node> children() {
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
