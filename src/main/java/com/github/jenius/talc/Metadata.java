package com.github.jenius.talc;

import com.github.jenius.component.Node;

import java.util.Objects;
import java.util.Optional;

public record Metadata(Node document, Summary summary, Optional<Node> infosOpt) {
  public Metadata {
    Objects.requireNonNull(document);
    Objects.requireNonNull(summary);
    Objects.requireNonNull(infosOpt);
  }
}
