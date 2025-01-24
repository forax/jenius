package com.github.jenius.talc;

import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Summary(String title, List<String> exercises) {
  public Summary {
    Objects.requireNonNull(title);
    exercises = List.copyOf(exercises);
  }

  static Summary extractSummaryFromIndex(Node document) throws IOException {
    var node = XML.transform(document, ComponentStyle.of(
        "title", (_, _, b) -> b.node("title")
    ).ignoreAllOthers());
    return new Summary(node.getFirstElement().text(), List.of());
  }


  static Summary extractSummaryFromFile(Node document) throws IOException {
    var node = XML.transform(document, ComponentStyle.of(Map.of(
        "td",  (_, _, b) -> b.node("td"),
        "title", (_, _, b) -> b.node("title"),
        "exercise", (_, attrs, b) -> b
            .node("exercise", children -> children
                .text(attrs.get("title")))
    )).ignoreAllOthers());
    var root = node.getFirstElement();
    return new Summary(
        root.text().strip(),
        root.childNodes().stream().skip(1).map(Node::text).toList());
  }

  public static Summary extractSummary(FileKind kind, Node document) throws IOException {
    return switch (kind) {
      case FILE -> extractSummaryFromFile(document);
      case INDEX -> extractSummaryFromIndex(document);
    };
  }
}
