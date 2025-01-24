package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratorTest {
  private static Path path(String filename) throws URISyntaxException {
    return Path.of(GeneratorTest.class.getResource(filename).toURI());
  }



  @Test
  public void test() throws URISyntaxException, IOException {
    var stylesheetPath = path("template.html");
    var file = path("td01.xumlv");
    var output = path(".").resolve("target", "td01.html");
    Files.createDirectories(output.getParent());

    var stylesheet = DocumentManager.readPathAsDocument(stylesheetPath);

    var documentManager = new DocumentManager();
    var document = documentManager.getDocument(FileKind.FILE, file);
    var summary = documentManager.getSummary(FileKind.FILE, file);

    try(var writer = Files.newBufferedWriter(output)) {
      Generator.generate(document, writer, stylesheet, summary);
    }
  }
}