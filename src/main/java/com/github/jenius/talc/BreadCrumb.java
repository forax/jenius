package com.github.jenius.talc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record BreadCrumb(List<String> titles, List<String> hrefs) {
  public BreadCrumb {
    titles = List.copyOf(titles);
    hrefs = List.copyOf(hrefs);
    if (titles.size() != hrefs.size()) {
      throw new IllegalArgumentException("titles and hrefs do not have the same size");
    }
  }

  @Override
  public String toString() {
    return IntStream.range(0, titles.size())
        .mapToObj(i -> titles.get(i) + " (" + hrefs.get(i) + ")")
        .collect(Collectors.joining(" :: "));
  }
}
