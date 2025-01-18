package com.github.jenius.component;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

@FunctionalInterface
public interface NodeBuilder {
  default NodeBuilder node(String name) {
    return node(name, CompactMap.of());
  }
  default NodeBuilder node(String name, String key1, Object value1) {
    return node(name, CompactMap.of(key1, value1));
  }
  default NodeBuilder node(String name, String key1, Object value1, String key2, Object value2) {
    return node(name, CompactMap.of(key1, value1, key2, value2));
  }
  default NodeBuilder node(String name, String key1, Object value1, String key2, Object value2, String key3, Object value3) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3));
  }
  default NodeBuilder node(String name, String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3, key4, value4));
  }

  default NodeBuilder node(String name, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(), children);
  }
  default NodeBuilder node(String name, String key1, Object value1, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1), children);
  }
  default NodeBuilder node(String name, String key1, Object value1, String key2, Object value2, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2), children);
  }
  default NodeBuilder node(String name, String key1, Object value1, String key2, Object value2, String key3, Object value3, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3), children);
  }
  default NodeBuilder node(String name, String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3, key4, value4), children);
  }

  default NodeBuilder node(String name, Map<? extends String, ? extends Object> map) {
    return node(name, map, _ -> {});
  }

  NodeBuilder node(String name, Map<? extends String, ? extends Object> map, Consumer<? super NodeBuilder> children);
}
