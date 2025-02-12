package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    var templateNode = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(root);

    var generator = new Generator(manager, mapping(), templateNode);
    generator.generate(file, output);
  }

  @Test
  public void generateIndex() throws URISyntaxException, IOException {
    var template = path("root/template.html");
    var root = path("root");
    var file = root.resolve("Java", "index.xumlv");
    var output = root.resolveSibling("target").resolve("Java", mapping().apply("index.xumlv"));
    Files.createDirectories(output.getParent());

    var templateNode = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(root);

    var generator = new Generator(manager, mapping(), templateNode);
    generator.generate(file, output);
  }

  @Test
  public void generateRootIndex() throws URISyntaxException, IOException {
    var template = path("root/template.html");
    var root = path("root");
    var file = root.resolve("index.xumlv");
    var output = root.resolveSibling("target").resolve(mapping().apply("index.xumlv"));
    Files.createDirectories(output.getParent());

    var templateNode = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(root);

    var generator = new Generator(manager, mapping(), templateNode);
    generator.generate(file, output);
  }

  @Test
  public void indexBreadcrumb() throws URISyntaxException {
    var root = path("root");
    var rootIndex = root.resolve("index.xumlv");
    var file = root.resolve("Java", "index.xumlv");

    var manager = new DocumentManager(root);
    var breadcrumb = manager.getBreadCrumb(file);
    var expected = new BreadCrumb(
        List.of("2007-2008", "Programmation Objet avec Java"),
        List.of(rootIndex, file));
    assertEquals(expected, breadcrumb);
  }

  @Test
  public void fileBreadcrumb() throws URISyntaxException {
    var root = path("root");
    var rootIndex = root.resolve("index.xumlv");
    var javaIndex = root.resolve("Java", "index.xumlv");
    var file = root.resolve("Java", "td01.xumlv");

    var manager = new DocumentManager(root);
    var breadcrumb = manager.getBreadCrumb(file);
    var expected = new BreadCrumb(
        List.of("2007-2008", "Programmation Objet avec Java"),
        List.of(rootIndex, javaIndex));
    assertEquals(expected, breadcrumb);
  }

  @Test
  public void rootFileBreadcrumb() throws URISyntaxException {
    var root = path("root");
    var rootIndex = root.resolve("index.xumlv");

    var manager = new DocumentManager(root);
    var breadcrumb = manager.getBreadCrumb(rootIndex);
    var expected = new BreadCrumb(
        List.of("2007-2008"),
        List.of(rootIndex));
    assertEquals(expected, breadcrumb);
  }

  @Test
  public void generateAll() throws URISyntaxException, IOException {
    var template = path("root/template.html");
    var dir = path("root");
    var dest = dir.resolveSibling("target");
    var privateDest = dir.resolveSibling("target").resolve("private");
    Files.createDirectories(dest);
    Files.createDirectories(privateDest);

    var planFactory = new PlanFactory(mapping());
    var plan = planFactory.diff(dir, dest, privateDest);
    plan.remove(template);
    //System.out.println(plan);

    var templateNode = DocumentManager.readPathAsDocument(template);
    var manager = new DocumentManager(dir);
    var generator = new Generator(manager, mapping(), templateNode);

    for(var entry : plan.statusMap().entrySet()) {
      var path = entry.getKey();
      for(var status : entry.getValue()) {
        var state = status.state();
        var destFile = status.destFile();
        if (Files.isDirectory(path)) {
          //System.out.println("create directory " + destFile);
          Files.createDirectories(destFile);
          continue;
        }
        var pathname = path.getFileName().toString();
        if (!pathname.endsWith(".xumlv")) {
          //System.out.println("copy to " + destFile);
          Files.copy(path, destFile, StandardCopyOption.REPLACE_EXISTING);
          continue;
        }
        //System.out.println("generate " + destFile + " " + state);
        generator.generate(path, destFile);
      }
    }
  }
}