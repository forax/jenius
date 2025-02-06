package com.github.jenius.talc;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.StringJoiner;

public record Plan(SequencedMap<Path, Status> statusMap) {
  public Plan {
    Objects.requireNonNull(statusMap);
    statusMap = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(statusMap));
  }

  @Override
  public String toString() {
    var joiner = new StringJoiner("\n");
    statusMap.forEach((path, status) -> {
      joiner.add(path + ": " + status);
    });
    return joiner.toString();
  }
}
