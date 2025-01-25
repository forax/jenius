package com.github.jenius.talc;

import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DocumentManager {
  private record BreadcrumbData(String name, Path path) {}

  private final Path root;
  private final HashMap<Path, Metadata> metadataMap = new HashMap<>();
  private final HashMap<Path, BreadcrumbData> breadcrumbMap = new HashMap<>();

  public DocumentManager(Path root) {
    this.root = Objects.requireNonNull(root);
  }

  static Node readPathAsDocument(Path path) throws IOException {
    try(var reader = Files.newBufferedReader(path)) {
      return XML.transform(reader);
    }
  }

  static Optional<Summary> extractSummary(Node document) {
      var node = XML.transform(document, ComponentStyle.of(Map.of(
          "td", (_, _, b) -> b.node("td"),
          "project", (_, _, b) -> b.node("project"),
          "title", (_, _, b) -> b.node("title"),
          "exercise", (_, attrs, b) -> b
              .node("exercise", children -> children
                  .text(attrs.get("title")))
      )).ignoreAllOthers());
      var td = node.getFirstElement();
      var titleOpt = td.find("title");
      if (titleOpt.isEmpty()) {
        return Optional.empty();
      }
      var title = titleOpt.orElseThrow().text().strip();
      var exercises = td.childNodes().stream()
            .filter(n -> !n.name().equals("title")).map(Node::text).toList();
      return Optional.of(new Summary(title, exercises));
  }

  static Summary defaultSummary(Optional<Summary> summaryOpt, Path path) {
    return summaryOpt
        .orElseGet(() -> new Summary(Utils.removeExtension(path.getFileName().toString()), List.of()));
  }

  private BreadcrumbData extractBreadcrumbData(Path dir) {
    System.err.println("extract name from " + dir);
    return breadcrumbMap.computeIfAbsent(dir, path -> {
      var indexPath = path.resolve("index.xumlv");
      if (!Files.exists(indexPath)) {
        return new BreadcrumbData(path.getFileName().toString(), path);
      }
      try {
        return new BreadcrumbData(getMetadata(indexPath).summary().title(), indexPath);
      } catch (IOException _) {
        return new BreadcrumbData(path.getFileName().toString(), path);
      }
    });
  }

  private void extractBreadcrumbs(Path dir, List<String> names, List<Path> hrefs) {
    var depth = 0;
    Path parent;
    for(var path = dir;; path = parent) {
      var breadcrumbData = extractBreadcrumbData(path);
      names.add(breadcrumbData.name);
      hrefs.add(breadcrumbData.path);
      if (path.equals(root)) {
        return;
      }
      parent = path.getParent();
      if (parent == null) {  // let's be safe
        return;
      }
    }
  }

  public BreadCrumb getBreadCrumb(Path path) {
    System.err.println("create " + path);
    var filename = path.getFileName().toString();
    var parent = path.getParent();
    if (filename.equals("index.xumlv")) {
      if (parent.equals(root)) {
        return new BreadCrumb(List.of(), List.of());
      }
      parent = parent.getParent();
    }
    var names = new ArrayList<String>();
    var hrefs = new ArrayList<Path>();
    extractBreadcrumbs(parent, names, hrefs);
    return new BreadCrumb(names, hrefs);
  }

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
