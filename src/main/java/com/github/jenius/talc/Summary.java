package com.github.jenius.talc;

import java.util.List;
import java.util.Objects;

public record Summary(String title, List<String> exercises) {
  public Summary {
    Objects.requireNonNull(title);
    exercises = List.copyOf(exercises);
  }
}
