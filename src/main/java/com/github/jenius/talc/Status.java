package com.github.jenius.talc;

import java.nio.file.Path;
import java.util.Objects;

public record Status(State state, Path destFile) {
  public enum State { UPDATED, ADDED, REMOVED }

  public Status {
    Objects.requireNonNull(state);
    Objects.requireNonNull(destFile);
  }
}