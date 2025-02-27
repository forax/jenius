package com.github.jenius.component;

import java.io.Reader;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface NodeBuilder {
  /// Replace the current node (but not its content) by an element (defined by a name)
  default NodeBuilder node(String name) {
    return node(name, CompactMap.of());
  }
  /// Replace the current node (but not its content) by an element (defined by a name and attributes)
  default NodeBuilder node(String name, String key1, String value1) {
    return node(name, CompactMap.of(key1, value1));
  }
  /// Replace the current node (but not its content) by an element (defined by a name and attributes)
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2) {
    return node(name, CompactMap.of(key1, value1, key2, value2));
  }
  /// Replace the current node (but not its content) by an element (defined by a name and attributes)
  default NodeBuilder node(String name, Map<String, String> map) {
    return node(name, map, _ -> {});
  }
  /// Replace the current node (but not its content) by an element (defined by a name and children nodes)
  default NodeBuilder node(String name, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(), children);
  }
  /// Replace the current node (but not its content) by an element (defined by a name, attributes and children nodes)
  default NodeBuilder node(String name, String key1, String value1, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1), children);
  }
  /// Replace the current node (but not its content) by an element (defined by a name, attributes and children nodes)
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2), children);
  }

  /// Replace the current node (but not its content) by an element (defined by a name, attributes and children nodes)
  NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children);

  /// Replace the current node by a text
  NodeBuilder text(String text);

  /// Replace the current node (but not its content) with the XML content of the reader
  NodeBuilder include(Reader reader);

  /// Replace the current node (but not its content) with the node
  NodeBuilder include(Node node);

  /// Collect the current node and its content into a [Node]
  /// and calls the consumer with that node and a node builder
  void collect(BiConsumer<? super Node, ? super NodeBuilder> consumer);

  /// Replace the current node with several nodes
  /// the content of the current node is preserved
  NodeBuilder fragment(Consumer<? super NodeBuilder> children);

  /// Hide the current node and its content
  void hide();

  /// Replace the current node (but not its content) by pre-nodes, followed by the content of the nodes
  /// followed by the post-nodes
  void around(Consumer<? super NodeBuilder> preBuilder, Consumer<? super NodeBuilder> postBuilder);
}
