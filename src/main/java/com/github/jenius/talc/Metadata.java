package com.github.jenius.talc;

import com.github.jenius.component.Node;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public interface Metadata {
  record File(Path path, Summary summary, Node document, Optional<Node> infosOpt) implements Metadata {
    public File {
      Objects.requireNonNull(path);
      Objects.requireNonNull(summary);
      Objects.requireNonNull(document);
      Objects.requireNonNull(infosOpt);
    }
  }
  record Dir(Path path, Summary summary) implements Metadata {
    public Dir {
      Objects.requireNonNull(path);
      Objects.requireNonNull(summary);
    }
  }

  Path path();
  Summary summary();
}
