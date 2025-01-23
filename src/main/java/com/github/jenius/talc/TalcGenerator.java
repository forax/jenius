package com.github.jenius.talc;

import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public class TalcGenerator {
  public static String extractTitleFromIndex(Reader reader) throws IOException {
    var node = XML.transform(reader, ComponentStyle.of(
        "title", (_, _, b) -> b.node("title")
    ).ignoreAllOthers());
    return node.getFirst().text();
  }

  public record Summary(String title, List<String> exercises) {}

  public static Summary extractSummary(Reader reader) throws IOException {
    var node = XML.transform(reader, ComponentStyle.of(Map.of(
        "td",  (_, _, b) -> b.node("td"),
        "title", (_, _, b) -> b.node("title"),
        "exercise", (_, attrs, b) -> b
                .node("exercise", children -> children
                    .text(attrs.get("title")))
    )).ignoreAllOthers());
    var root = node.getFirst();
    return new Summary(
        root.text().strip(),
        root.children().stream().skip(1).map(Node::text).toList());
  }
}
