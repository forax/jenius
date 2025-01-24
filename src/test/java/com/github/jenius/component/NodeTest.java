package com.github.jenius.component;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {
  @Test
  public void createDocument() {
    var document = Node.createDocument();
    assertAll(
        () -> assertEquals(Map.of(), document.attributes()),
        () -> assertEquals(List.of(), document.childNodes()),
        () -> assertEquals(List.of(), document.elements())
    );
  }

  @Test
  public void createNode() {
    var document = Node.createDocument();
    var node = document.createNode("div", Map.of("foo", "bar"));
    document.appendChild(node);
    assertAll(
        () -> assertEquals(Map.of("foo", "bar"), node.attributes()),
        () -> assertEquals(1, document.childNodes().size()),
        () -> assertEquals(1, document.elements().size()),
        () -> assertEquals("div", document.getFirstElement().name())
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
        () -> assertEquals("span", node.getFirstElement().name())
    );
  }

  @Test
  public void appendText() {
    var document = Node.createDocument();
    var node = document.createNode("div", Map.of("foo", "bar"));
    document.appendChild(node);
    node.appendText("hello");
    assertEquals("hello", node.text());
  }

  @Test
  public void transformToNode() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var node = XML.transform(new StringReader(input), style);
    System.err.println(node.toDebugString());
    var root = node.getFirstElement();
    assertAll(
        () -> assertEquals("foo", root.name()),
        () -> assertEquals("bar", root.getFirstElement().name()),
        () -> assertEquals(Map.of("glut", "true"), root.getFirstElement().attributes()),
        () -> assertEquals("This is a text", root.getFirstElement().text().strip())
    );
  }
}