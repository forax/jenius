package com.github.jenius.component;

import java.io.Reader;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface NodeBuilder {
  default NodeBuilder node(String name) {
    return node(name, CompactMap.of());
  }
  default NodeBuilder node(String name, String key1, String value1) {
    return node(name, CompactMap.of(key1, value1));
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2) {
    return node(name, CompactMap.of(key1, value1, key2, value2));
  }
  default NodeBuilder node(String name, Map<String, String> map) {
    return node(name, map, _ -> {});
  }

  default NodeBuilder node(String name, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(), children);
  }
  default NodeBuilder node(String name, String key1, String value1, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1), children);
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2), children);
  }

  NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children);

  NodeBuilder text(String text);

  NodeBuilder include(Reader reader);

  NodeBuilder include(Node node);

  void collect(BiConsumer<? super Node, ? super NodeBuilder> consumer);

  NodeBuilder fragment(Consumer<? super NodeBuilder> children);

  void hide();
}
