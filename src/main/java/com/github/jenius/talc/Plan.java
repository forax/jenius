package com.github.jenius.talc;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.stream.Collectors;

public final class Plan {
  public record Entry(Path path, Status status) {}

  private final LinkedHashMap<Path, List<Status>> planMap = new LinkedHashMap<>();

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Plan plan && planMap.equals(plan.planMap);
  }

  @Override
  public int hashCode() {
    return planMap.hashCode();
  }

  public void add(Path path, Status status) {
    Objects.requireNonNull(path);
    Objects.requireNonNull(status);
    planMap.merge(path, List.of(status), (l1, l2) -> {
      if (l1.size() != 1 || l2.size() != 1) {
        throw new AssertionError();
      }
      return List.of(l1.getFirst(), l2.getFirst());
    });
  }

  public void remove(Path path) {
    Objects.requireNonNull(path);
    planMap.remove(path);
  }

  public SequencedMap<Path, List<Status>> statusMap() {
    return Collections.unmodifiableSequencedMap(planMap);
  }

  @Override
  public String toString() {
    return planMap.entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(status -> e.getKey() + ": " + status))
        .collect(Collectors.joining("\n"));
  }
}
