package com.github.jenius.component;

import java.io.Reader;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, String key3, String value3) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3));
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3, key4, value4));
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
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, String key3, String value3, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3), children);
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3, key4, value4), children);
  }
  default NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) {
    return node(name, map, null, children);
  }

  default NodeBuilder node(String name, UnaryOperator<String> withText, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(), withText, children);
  }
  default NodeBuilder node(String name, String key1, String value1,  UnaryOperator<String> withText, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1), withText, children);
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2,  UnaryOperator<String> withText, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2), withText, children);
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, String key3, String value3,  UnaryOperator<String> withText, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3), withText, children);
  }
  default NodeBuilder node(String name, String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4,  UnaryOperator<String> withText, Consumer<? super NodeBuilder> children) {
    return node(name, CompactMap.of(key1, value1, key2, value2, key3, value3, key4, value4), withText, children);
  }

  NodeBuilder node(String name, Map<String, String> map, UnaryOperator<String> withText, Consumer<? super NodeBuilder> children);

  NodeBuilder text(String text);

  NodeBuilder include(Reader reader);
}
