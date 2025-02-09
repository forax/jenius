package com.github.jenius.component;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {
  @Test
  public void shouldCreateDocument() {
    var document = Node.createDocument();
    assertAll(
        () -> assertEquals(Map.of(), document.attributes()),
        () -> assertEquals(List.of(), document.childNodes()),
        () -> assertEquals(List.of(), document.elements())
    );
  }

  @Test
  public void shouldCreateEmptyNode() {
    var document = Node.createDocument();
    var node = document.createNode("div");

    assertAll(
        () -> assertEquals("div", node.name()),
        () -> assertTrue(node.attributes().isEmpty()),
        () -> assertTrue(node.childNodes().isEmpty())
    );
  }

  @Test
  public void shouldCreateEmptyTextNode() {
    var document = Node.createDocument();
    var node = document.createNode("div");

    assertEquals("", node.text());
  }

  @Test
  public void shouldCreateNodeWithAttributes() {
    var document = Node.createDocument();
    var attrs = Map.of("class", "container", "id", "main");
    var node = document.createNode("div", attrs);

    assertAll(
        () -> assertEquals("div", node.name()),
        () -> assertEquals(attrs, node.attributes()),
        () -> assertEquals("container", node.attributes().get("class")),
        () -> assertEquals("main", node.attributes().get("id")),
        () -> assertEquals(2, node.attributes().size())
    );

  }

  @Test
  public void shouldAccessFromDocument() {
    var document = Node.createDocument();
    var node = document.createNode("div", Map.of("foo", "bar"));
    document.appendChild(node);

    assertAll(
        () -> assertEquals(1, document.childNodes().size()),
        () -> assertEquals(1, document.elements().size()),
        () -> assertEquals("div", document.getFirstElement().orElseThrow().name())
    );
  }

  @Test
  public void createNodeWithChildNodes() {
    var document = Node.createDocument();
    var child = document.createNode("span");
    var node = document.createNode("div", Map.of("foo", "bar"), List.of(child));
    document.appendChild(node);

    assertAll(
        () -> assertEquals(Map.of("foo", "bar"), node.attributes()),
        () -> assertEquals(1, node.childNodes().size()),
        () -> assertEquals(1, document.childNodes().size()),
        () -> assertEquals(1, document.elements().size()),
        () -> assertEquals("span", node.getFirstElement().orElseThrow().name())
    );
  }

  @Test
  public void shouldCreateNodeWithChildNode2() {
    var document = Node.createDocument();
    var child1 = document.createNode("span");
    var child2 = document.createNode("p");
    var parent = document.createNode("div", Map.of(), List.of(child1, child2));

    assertAll(
        () -> assertEquals(2, parent.childNodes().size()),
        () -> assertEquals("span", parent.childNodes().get(0).name()),
        () -> assertEquals("p", parent.childNodes().get(1).name())
    );

  }

  @Test
  public void shouldAppendChildNode() {
    var document = Node.createDocument();
    var parent = document.createNode("div");
    var child = document.createNode("span");
    parent.appendChild(child);

    assertAll(
        () -> assertEquals(1, parent.childNodes().size()),
        () -> assertEquals("span", parent.childNodes().getFirst().name())
    );

  }

  @Test
  public void shouldReturnFirstElement() {
    var document = Node.createDocument();
    var parent = document.createNode("div");
    var child1 = document.createNode("span");
    var child2 = document.createNode("p");
    parent.appendChild(child1);
    parent.appendChild(child2);

    var firstElement = parent.getFirstElement();
    assertEquals("span", firstElement.orElseThrow().name());
  }

  @Test
  public void shouldSupportAttributeMapOperations() {
    var document = Node.createDocument();
    var attrs = Map.of("class", "container", "id", "main");
    var node = document.createNode("div", attrs);
    var attributes = node.attributes();

    assertAll(
        () -> assertEquals(2, attributes.size()),
        () -> assertTrue(attributes.containsKey("class")),
        () -> assertFalse(attributes.containsKey("nonexistent")),
        () -> assertEquals("container", attributes.get("class")),
        () -> assertNull(attributes.get("nonexistent")),
        () -> assertEquals("container", attributes.getOrDefault("class", "default")),
        () -> assertEquals("default", attributes.getOrDefault("nonexistent", "default"))
    );

  }

  @Test
  public void shouldProvideStringRepresentation() {
    var document = Node.createDocument();
    var div = document.createNode("div", Map.of("class", "container"));
    var span = div.createNode("span");
    span.appendText("Hello");

    var result = div.toString();
    assertEquals("""
        Node: div [Type: Element]    Attribute: class = container
      """, result);
  }

  @Test
  public void shouldAppendTextContent() {
    var document = Node.createDocument();
    var node = document.createNode("div");
    node.appendText("Hello World");

    assertEquals("Hello World", node.text());
  }

  @Test
  public void shouldAppendTextContent2() {
    var document = Node.createDocument();
    var node = document.createNode("div", Map.of("foo", "bar"));
    document.appendChild(node);
    node.appendText("hello");

    assertEquals("hello", node.text());
  }

  @Test
  public void shouldTransformNodeCorrectly() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var document = XML.transform(new StringReader(input), style);
    var root = document.getFirstElement().orElseThrow();

    assertAll(
        () -> assertEquals("foo", root.name()),
        () -> assertEquals("bar", root.getFirstElement().orElseThrow().name()),
        () -> assertEquals(Map.of("glut", "true"), root.getFirstElement().orElseThrow().attributes()),
        () -> assertEquals("This is a text", root.getFirstElement().orElseThrow().text().strip())
    );
  }

  @Test
  public void shouldFindNodeByPath() {
    var document = Node.createDocument();
    var div = document.createNode("div");
    var span = document.createNode("span");
    span.appendChild(document.createNode("p"));
    div.appendChild(span);
    document.appendChild(div);

    var found = document.path("div", "span", "p");
    assertEquals("p", found.name());
  }

  @Test
  public void shouldFindTheCorrectNodeUsingPath() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var document = XML.transform(new StringReader(input));
    assertAll(
        () -> assertEquals("foo", document.path("foo").name()),
        () -> assertEquals("bar", document.path("foo", "bar").name())
    );
  }

  @Test
  public void shouldThrowExceptionForInvalidPath() {
    var document = Node.createDocument();
    var div = document.createNode("div");
    document.appendChild(div);

    assertThrows(IllegalStateException.class, () ->
        document.path("div", "nonexistent")
    );
  }

  @Test
  public void shouldRemoveNodeFromParent() {
    var document = Node.createDocument();
    var parent = document.createNode("div");
    var child = document.createNode("span");
    parent.appendChild(child);
    child.removeFromParent();

    assertTrue(parent.childNodes().isEmpty());
  }

  @Test
  public void shouldThrowExceptionWhenRemovingNodeWithNoParent() {
    var document = Node.createDocument();
    var node = document.createNode("div");

    assertThrows(IllegalStateException.class, node::removeFromParent);
  }

  @Test
  public void shouldFindNodeByName() {
    var document = Node.createDocument();
    var div = document.createNode("div");
    var span = document.createNode("span");
    var p = document.createNode("p");
    p.appendText("Find me");
    span.appendChild(p);
    div.appendChild(span);
    document.appendChild(div);

    var found = document.find("p");
    assertEquals("Find me", found.orElseThrow().text());
  }
}