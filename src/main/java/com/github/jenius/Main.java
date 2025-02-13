package com.github.jenius;

import com.github.jenius.component.Node;
import com.github.jenius.component.XML;
import com.github.jenius.talc.DocumentManager;
import com.github.jenius.talc.Generator;
import com.github.jenius.talc.Plan;
import com.github.jenius.talc.PlanFactory;
import com.github.jenius.talc.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

  private static void deleteFiles(Plan plan) throws IOException {
    for (var entry : plan.statusMap().reversed().entrySet()) {
      for(var status : entry.getValue()) {
        switch (status.state()) {
          case UPDATED, ADDED -> {}
          case REMOVED -> {
            Files.delete(status.destFile());
          }
        }
      }
    }
  }

  private static void generateFiles(Generator generator, Plan plan) throws IOException {
    for (var entry : plan.statusMap().entrySet()) {
      var path = entry.getKey();
      for (var status : entry.getValue()) {
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
              Files.copy(path, destFile, StandardCopyOption.REPLACE_EXISTING);
              continue;
            }
            System.out.println("generate " + destFile + " " + state);
            generator.generate(path, destFile, status.kind() == Status.Kind.PRIVATE);
          }
        }
      }
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.err.println("  jenius sourceDir destinationDir privateDir template");
      System.exit(1);
      return;
    }

    var mapping = (UnaryOperator<String>) Main::mapping;
    var planFactory = new PlanFactory(mapping);
    var dir = Path.of(args[0]);
    var dest = Path.of(args[1]);
    var privateDest = Path.of(args[2]);
    var template = Path.of(args[3]);

    System.out.println("INFO config dir:" + dir.toAbsolutePath());
    System.out.println("INFO config dest:" + dest.toAbsolutePath());
    System.out.println("INFO config private dest:" + privateDest.toAbsolutePath());
    System.out.println("INFO config template:" + template.toAbsolutePath());

    Node templateNode;
    try(var reader = Files.newBufferedReader(template)) {
      templateNode = XML.transform(reader);
    }

    // do a diff between dir, dest and private dest
    var plan = planFactory.diff(dir, dest, privateDest);
    plan.remove(template);  // skip template

    if (plan.statusMap().isEmpty()) {
      System.out.println("nothing to do !");
      return;
    }

    // remove supplementary files in dest
    deleteFiles(plan);

    // generates modified files in dest
    var manager = new DocumentManager(dir);
    var generator = new Generator(manager, mapping, templateNode);
    generateFiles(generator, plan);
  }
}
