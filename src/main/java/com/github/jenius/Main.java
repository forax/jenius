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
import java.util.Map;
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

    System.out.println("jenius: dir " + dir.toAbsolutePath() + " dest " + dest.toAbsolutePath() + " template " + template);

    Node templateNode;
    try(var reader = Files.newBufferedReader(template)) {
      templateNode = XML.transform(reader);
    }

    var plan = planFactory.diff(dir, dest);
    for (var entry : plan.statusMap().entrySet()) {
      var path = entry.getKey();
      if (path.equals(template)) {
        System.out.println("skipped template.html");
        continue;
      }
      var status = entry.getValue();
      var state = status.state();
      var destFile = status.destFile();
      switch (state) {
        case REMOVED -> {
        }
        case UPDATED, ADDED -> {
          if (Files.isDirectory(path)) {
            Files.createDirectories(destFile);
            continue;
          }
          var manager = new DocumentManager(dir);
          var generator = new Generator(manager, mapping, templateNode);

          System.out.println("generate " + path + " to " + destFile + " " + state);
          generator.generate(path, destFile);
        }
      }
    }
  }
}
