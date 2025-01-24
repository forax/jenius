package com.github.jenius.talc;

import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class DocumentManager {
  private record Data(Node document, Summary summary) {}

  private final LinkedHashMap<Path, Data> modifiedPathMap = new LinkedHashMap<>();

  private static Node readPathAsDocument(Path path) throws IOException {
    try(var reader = Files.newBufferedReader(path)) {
      return XML.transform(reader);
    }
  }

  private static Summary extractSummary(FileKind kind, Node document) throws IOException {
    return switch (kind) {
      case FILE -> TalcGenerator.extractSummaryFromFile(document);
      case INDEX -> TalcGenerator.extractSummaryFromIndex(document);
    };
  }

  private Data getData(FileKind kind, Path path) throws IOException {
    try {
      return modifiedPathMap.computeIfAbsent(path, p -> {
        try {
          var document = readPathAsDocument(path);
          var summary = extractSummary(kind, document);
          return new Data(document, summary);
        }  catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  public Node getDocument(FileKind kind, Path path) throws IOException {
    Objects.requireNonNull(kind);
    Objects.requireNonNull(path);
    return getData(kind, path).document;
  }

  public Summary getSummary(FileKind kind, Path path) throws IOException {
    Objects.requireNonNull(kind);
    Objects.requireNonNull(path);
    return getData(kind, path).summary;
  }
}
