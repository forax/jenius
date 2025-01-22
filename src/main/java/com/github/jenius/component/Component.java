package com.github.jenius.component;

import java.util.Map;

@FunctionalInterface
public interface Component {
  void render(String name, Map<String, String> attributes, NodeBuilder nodeBuilder);

  static Component of(Component component) {
    return component;
  }

  static Component identity() {
    return (name, attributes, nodeBuilder) -> nodeBuilder.node(name, attributes);
  }

  static Component ignore() {
    return (_, _, _) -> {};
  }
}
