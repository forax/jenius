package com.github.jenius.talc;

import java.nio.file.Path;
import java.util.Objects;

public record Status(State state, Kind kind, Path destFile) {
  public enum State { UPDATED, ADDED, REMOVED }
  public enum Kind { PUBLIC, PRIVATE }

  public Status {
    Objects.requireNonNull(state);
    Objects.requireNonNull(kind);
    Objects.requireNonNull(destFile);
  }
}