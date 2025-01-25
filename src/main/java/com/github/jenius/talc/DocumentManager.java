package com.github.jenius.talc;

import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DocumentManager {
  private final HashMap<Path, Metadata> metadataMap = new HashMap<>();
  private final HashMap<Path, Metadata> breadcrumbMap = new HashMap<>();

  public static Node readPathAsDocument(Path path) throws IOException {
    try(var reader = Files.newBufferedReader(path)) {
      return XML.transform(reader);
    }
  }

  static Optional<Summary> extractSummary(Node document) {
      var node = XML.transform(document, ComponentStyle.of(Map.of(
          "td", (_, _, b) -> b.node("td"),
          "title", (_, _, b) -> b.node("title"),
          "exercise", (_, attrs, b) -> b
              .node("exercise", children -> children
                  .text(attrs.get("title")))
      )).ignoreAllOthers());
      var titleOpt = node.find("title");
      return titleOpt.map(title -> new Summary(
              title.text().strip(),
              title.childNodes().stream().skip(1).map(Node::text).toList()));

  }

  static Summary defaultSummary(Optional<Summary> summaryOpt, Path path) {
    return summaryOpt
        .orElseGet(() -> new Summary(Utils.removeExtension(path.getFileName().toString()), List.of()));
  }

  /*BreadCrumb getBreadCrumb(Path dir) {

  }*/

  public Metadata getMetadata(Path path) throws IOException {
    Objects.requireNonNull(path);
    try {
      return metadataMap.computeIfAbsent(path, p -> {
        try {
          var document = readPathAsDocument(path);
          var summary = defaultSummary(extractSummary(document), path);
          document.find("title").ifPresent(Node::removeFromParent);      // remove title
          var infosOpt = document.find("infos");
          infosOpt.ifPresent(Node::removeFromParent);  // remove infos
          return new Metadata(document, summary, infosOpt);
        }  catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
