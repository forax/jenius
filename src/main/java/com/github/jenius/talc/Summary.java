package com.github.jenius.talc;

import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Summary(String title, List<String> exercises) {
  public Summary {
    Objects.requireNonNull(title, "title is null");
    exercises = List.copyOf(exercises);
  }
}
