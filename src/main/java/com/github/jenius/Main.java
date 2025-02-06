package com.github.jenius;

import com.github.jenius.component.Node;
import com.github.jenius.component.XML;
import com.github.jenius.talc.DocumentManager;
import com.github.jenius.talc.Generator;
import com.github.jenius.talc.PlanFactory;
import com.github.jenius.talc.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.UnaryOperator;

public class Main {
  private static String mapping(String filename) {
    var index = filename.lastIndexOf('.');
    if (index == -1) {
      return filename;
    }
    var extension = filename.substring(index + 1);
    var newExtension = switch (extension) {
      case "xumlv" -> "html";
      default -> extension;
    };
    return filename.substring(0, index) + "." + newExtension;
  }

  private static void deleteFiles(SequencedMap<Path, Status> statusMap) throws IOException {
    for (var status : statusMap.sequencedValues().reversed()) {
      switch (status.state()) {
        case UPDATED, ADDED -> {}
        case REMOVED -> {
          Files.delete(status.destFile());
        }
      }
    }
  }

  private static void generateFiles(Generator generator, SequencedMap<Path, Status> statusMap) throws IOException {
    for (var entry : statusMap.entrySet()) {
      var path = entry.getKey();
      var status = entry.getValue();
      var state = status.state();
      switch (state) {
        case REMOVED -> {}
        case UPDATED, ADDED -> {
          var destFile = status.destFile();
          if (Files.isDirectory(path)) {
            System.out.println("create directory " + destFile);
            Files.createDirectories(destFile);
            continue;
          }
          var pathname = path.getFileName().toString();
          if (!pathname.endsWith(".xumlv")) {
            System.out.println("copy to " + destFile);
            Files.copy(path, destFile);
            continue;
          }
          System.out.println("generate " + destFile + " " + state);
          generator.generate(path, destFile);
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.err.println("  jenius sourceDir destinationDir template");
      System.exit(1);
      return;
    }

    var mapping = (UnaryOperator<String>) Main::mapping;
    var planFactory = new PlanFactory(mapping);
    var dir = Path.of(args[0]);
    var dest = Path.of(args[1]);
    var template = Path.of(args[2]);

    System.out.println("INFO config: dir:" + dir.toAbsolutePath() + " dest:" + dest.toAbsolutePath() + " template:" + template);

    Node templateNode;
    try(var reader = Files.newBufferedReader(template)) {
      templateNode = XML.transform(reader);
    }

    // do a diff between dir and dest
    var plan = planFactory.diff(dir, dest);
    var statusMap = new LinkedHashMap<>(plan.statusMap());
    statusMap.remove(template);  // skip template.html

    if (statusMap.isEmpty()) {
      System.out.println("nothing to do !");
      return;
    }

    // remove supplementary files in dest
    deleteFiles(statusMap);

    // generates modified files in dest
    var manager = new DocumentManager(dir);
    var generator = new Generator(manager, mapping, templateNode);
    generateFiles(generator, statusMap);
  }
}
