package com.github.jenius.talc;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record BreadCrumb(List<String> names, List<Path> hrefs) {
  public BreadCrumb {
    names = List.copyOf(names);
    hrefs = List.copyOf(hrefs);
    if (names.size() != hrefs.size()) {
      throw new IllegalArgumentException("titles and hrefs do not have the same size");
    }
  }

  @Override
  public String toString() {
    return IntStream.range(0, names.size())
        .mapToObj(i -> names.get(i) + " (" + hrefs.get(i) + ")")
        .collect(Collectors.joining(" :: "));
  }
}
