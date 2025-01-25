package com.github.jenius.talc;

import java.util.List;
import java.util.Objects;

public record Summary(String title, List<String> subsections) {
  public Summary {
    Objects.requireNonNull(title, "title is null");
    subsections = List.copyOf(subsections);
  }

  public Summary(String title) {
    this(title, List.of());
  }
}
