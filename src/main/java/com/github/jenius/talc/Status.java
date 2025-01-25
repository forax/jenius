package com.github.jenius.talc;

import java.nio.file.Path;
import java.util.Objects;

public sealed interface Status {
  enum EnumStatus implements Status { ADDED, REMOVED }
  record Updated(Path destFile) implements Status {
    public Updated {
      Objects.requireNonNull(destFile);
    }
  }
}