package com.github.jenius.component;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {
  @Test
  public void createDocument() {
    var document = Node.createDocument();
    assertAll(
        () -> assertEquals(Map.of(), document.attributes()),
        () -> assertEquals(List.of(), document.children())
    );
  }

  @Test
  public void createNode() {
    var document = Node.createDocument();
    var node = document.createNode("div", Map.of("foo", "bar"));
    document.appendChild(node);
    assertAll(
        () -> assertEquals(Map.of("foo", "bar"), node.attributes()),
        () -> assertEquals(1, document.children().size()),
        () -> assertEquals("div", document.getFirst().name())
        );
  }

  @Test
  public void createNodeWithChildren() {
    var document = Node.createDocument();
    var child = document.createNode("span");
    var node = document.createNode("div", Map.of("foo", "bar"), List.of(child));
    document.appendChild(node);
    assertAll(
        () -> assertEquals(Map.of("foo", "bar"), node.attributes()),
        () -> assertEquals(1, node.children().size()),
        () -> assertEquals("span", node.getFirst().name())
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
}