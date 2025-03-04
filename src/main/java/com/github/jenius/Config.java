package com.github.jenius;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

record Config(boolean force, boolean watch, Path dir, Path dest, Path privateDest, Path template) {
  private static final class Option {
    private boolean force;
    private boolean watch;
  }

  private static void parseArgs(String[] args, List<Path> files, Option option) {
    for(String arg : args) {
      switch (arg) {
        case "--force" -> option.force = true;
        case "--watch" -> option.watch = true;
        default -> files.add(Path.of(arg));
      }
    }
  }

  private static Config newConfig(Option option, Path dir, Path dest, Path privateDest, Path template) {
    return new Config(option.force, option.watch, dir, dest, privateDest, template);
  }

  public static Config parseConfig(String[] args) {
    var files = new ArrayList<Path>();
    var option = new Option();
    parseArgs(args, files, option);
    return switch (files.size()) {
      case 3 -> newConfig(option, files.get(0), files.get(1), null, files.get(2));
      case 4 -> newConfig(option, files.get(0), files.get(1), files.get(2), files.get(3));
      default -> {
        System.err.println("""
            jenius [options] sourceDir destinationDir [privateDir] template.html
            
              options:
                --force generate all files (independently of if a source file is updated)
                --watch wait and generate in loop when at least one of the source files change
            """);
        System.exit(1);
        throw new AssertionError();
      }
    };
  }

  public void displayConfig() {
    System.out.println("INFO config dir: " + dir.toAbsolutePath());
    System.out.println("INFO config dest: " + dest.toAbsolutePath());
    if (privateDest != null) {
      System.out.println("INFO config private dest: " + privateDest.toAbsolutePath());
    }
    System.out.println("INFO config template: " + template.toAbsolutePath());
    if (force) {
      System.out.println("INFO config force: true");
    }
  }
}