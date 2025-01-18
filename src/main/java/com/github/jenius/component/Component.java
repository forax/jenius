package com.github.jenius.component;

import java.util.Map;

public interface Component {
  void render(String name, Map<String, String> attributes, NodeBuilder nodeBuilder);
}
