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
import java.util.function.Predicate;

public final class DocumentManager {
  private final Path root;
  private final HashMap<Path, Metadata.File> fileMetadataMap = new HashMap<>();
  private final HashMap<Path, Metadata> metadataMap = new HashMap<>();

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
            .filter(n -> !n.name().equals("title"))
            .map(n -> n.text().strip())
            .filter(Predicate.not(String::isEmpty))
            .toList();
      return Optional.of(new Summary(title, exercises));
  }

  static Summary defaultSummary(Optional<Summary> summaryOpt, String filename) {
    return summaryOpt
        .orElseGet(() -> new Summary(Utils.removeExtension(filename)));
  }

  private void extractBreadcrumbs(Path dir, List<String> names, List<Path> hrefs) {
    Path parent;
    for(var path = dir;; path = parent) {
      var breadcrumbData = getMetadata(path);
      names.add(breadcrumbData.summary().title());
      hrefs.add(breadcrumbData.path());
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
    return new BreadCrumb(names.reversed(), hrefs.reversed());
  }

  public Metadata getMetadata(Path path) {
    return metadataMap.computeIfAbsent(path, p -> {
      if (Files.isDirectory(p)) {
        var indexPath = p.resolve("index.xumlv");
        if (!Files.exists(indexPath)) {
          var dirName = Utils.removeExtension(p.getFileName().toString());
          return new Metadata.Dir(p, new Summary(dirName));
        }
        p = indexPath;
      }
      try {
        return getFileMetadata(p);
      } catch (IOException _) {
        var dirName = Utils.removeExtension(p.getFileName().toString());
        return new Metadata.Dir(p, new Summary(dirName));
      }
    });
  }

  public Metadata.File getFileMetadata(Path path) throws IOException {
    Objects.requireNonNull(path);
    assert !Files.isDirectory(path);
    try {
      return fileMetadataMap.computeIfAbsent(path, p -> {
        try {
          var document = readPathAsDocument(p);
          var summary = defaultSummary(extractSummary(document), p.getFileName().toString());
          document.find("title").ifPresent(Node::removeFromParent);  // remove title
          var infosOpt = document.find("infos");
          infosOpt.ifPresent(Node::removeFromParent);  // remove infos
          return new Metadata.File(p, summary, document, infosOpt);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
