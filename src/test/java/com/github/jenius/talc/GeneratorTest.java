package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratorTest {
  private static Path path(String filename) throws URISyntaxException {
    return Path.of(GeneratorTest.class.getResource(filename).toURI());
  }

  private static UnaryOperator<String> mapping() {
    return name -> {
      var index = name.lastIndexOf('.');
      if (index == -1) {
        return name;
      }
      var extension = name.substring(index + 1);
      var newExtension = switch (extension) {
        case "xumlv" -> "html";
        default -> extension;
      };
      return name.substring(0, index) + "." + newExtension;
    };
  }

  @Test
  public void generateFile() throws URISyntaxException, IOException {
    var template = path("root/template.html");
    var root = path("root");
    var file = root.resolve("Java", "td01.xumlv");
    var output = root.resolveSibling("target").resolve("Java", mapping().apply("td01.xumlv"));
    Files.createDirectories(output.getParent());

    var stylesheet = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(root);

    var generator = new Generator(manager, mapping(), stylesheet);
    generator.generate(file, output);
  }

  @Test
  public void generateIndex() throws URISyntaxException, IOException {
    var template = path("root/template.html");
    var root = path("root");
    var file = root.resolve("Java", "index.xumlv");
    var output = root.resolveSibling("target").resolve("Java", mapping().apply("index.xumlv"));
    Files.createDirectories(output.getParent());

    var stylesheet = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(root);

    var generator = new Generator(manager, mapping(), stylesheet);
    generator.generate(file, output);
  }

  @Test
  public void generateRootIndex() throws URISyntaxException, IOException {
    var template = path("root/template.html");
    var root = path("root");
    var file = root.resolve("index.xumlv");
    var output = root.resolveSibling("target").resolve(mapping().apply("index.xumlv"));
    Files.createDirectories(output.getParent());

    var stylesheet = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(root);

    var generator = new Generator(manager, mapping(), stylesheet);
    generator.generate(file, output);
  }
}