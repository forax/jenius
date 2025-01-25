package com.github.jenius.talc;

import java.util.List;
import java.util.Objects;

public record Summary(String title, List<String> exercises) {
  public Summary {
    Objects.requireNonNull(title, "title is null");
    exercises = List.copyOf(exercises);
  }
}
