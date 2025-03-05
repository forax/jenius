package com.github.jenius;

import com.github.jenius.component.Node;
import com.github.jenius.component.XML;
import com.github.jenius.talc.DocumentManager;
import com.github.jenius.talc.Generator;
import com.github.jenius.talc.Plan;
import com.github.jenius.talc.PlanFactory;
import com.github.jenius.talc.Status;
import com.sun.net.httpserver.SimpleFileServer;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchKey;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.stream.Collectors.toSet;

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
    IOException ioException = null;
    for (var entry : plan.statusMap().reversed().entrySet()) {
      for(var status : entry.getValue()) {
        switch (status.state()) {
          case UPDATED, ADDED -> {}
          case REMOVED -> {
            try {
              Files.delete(status.destFile());
            } catch (IOException e) {
              System.err.println("i/o error " + e.getMessage());
              if (ioException == null) {
                ioException = e;
              } else {
                ioException.addSuppressed(e);
              }
            }
          }
        }
      }
    }
    if (ioException != null) {
      throw ioException;
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

  private static Plan executePlan(PlanFactory planFactory, Path dir, Path dest, Path privateDest, Path template) throws IOException {
    Node templateNode;
    try(var reader = Files.newBufferedReader(template)) {
      templateNode = XML.transform(reader);
    }

    // do a diff between dir, dest and private dest
    var plan = planFactory.diff(dir, dest, privateDest);
    plan.remove(template);  // skip template

    if (plan.statusMap().isEmpty()) {
      System.out.println("nothing to do !");
      return plan;
    }

    // remove supplementary files in dest
    deleteFiles(plan);

    // generates modified files in dest
    var manager = new DocumentManager(dir);
    var generator = new Generator(manager, Main::mapping, templateNode);
    generateFiles(generator, plan);
    return plan;
  }

  private static Set<Path> scanDirectoriesToWatch(Path dir) throws IOException {
    try(var stream = Files.walk(dir)) {
      return stream.filter(Files::isDirectory).collect(toSet());
    }
  }

  private static Set<Path> extractDirectoriesFromPlan(Plan plan) {
    var directories = new HashSet<Path>();
    for (var entry : plan.statusMap().entrySet()) {
      var path = entry.getKey();
      for (var status : entry.getValue()) {
        var state = status.state();
        switch (state) {
          case REMOVED -> {}
          case UPDATED, ADDED -> {
            if (Files.isDirectory(path)) {
              directories.add(path);
            }
          }
        }
      }
    }
    return directories;
  }

  private static void watch(boolean force, Path dir, Path dest, Path privateDest, Path template) throws IOException {
    var watchService = FileSystems.getDefault().newWatchService();
    var directories = scanDirectoriesToWatch(dir);
    for(;;) {
      var planFactory = new PlanFactory(Main::mapping, force);
      var plan = executePlan(planFactory, dir, dest, privateDest, template);

      directories.addAll(extractDirectoriesFromPlan(plan));
      for(var directory : directories) {
        directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
      }
      directories = new HashSet<>();

      System.out.println("wait watching " + dir);
      WatchKey key;
      try {
        key = watchService.take();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      System.out.println(key.watchable() + " is modified");
      key.pollEvents();  // empty events
      key.reset();  // continue to watch the corresponding directory
    }
  }

  private static boolean isPrivateDestIncluded(Path dest, Path privateDest) {
    for(var path : privateDest) {
      if (path.equals(dest)) {
        return true;
      }
    }
    return false;
  }

  private static void serve(Path dest, Path privateDest) {
    var serveDir = privateDest == null ? dest : isPrivateDestIncluded(dest, privateDest) ? dest : privateDest;
    var localHost = InetAddress.getLoopbackAddress();
    var socketAddress = new InetSocketAddress(localHost, 8080);
    var rootDir = serveDir.toAbsolutePath();
    var server = SimpleFileServer.createFileServer(socketAddress, rootDir, OutputLevel.NONE);
    server.start();
    System.out.println("serve " + rootDir + " at http://" + localHost.getHostAddress() + ':' + socketAddress.getPort());
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    var config = Config.parseConfig(args);
    config.displayConfig();
    var force = config.force();
    var watch = config.watch();
    var serve = config.serve();
    var dir = config.dir();
    var dest = config.dest();
    var privateDest = config.privateDest();
    var template = config.template();

    if (serve) {
      serve(dest, privateDest);
    }
    if (watch) {
      watch(force, dir, dest, privateDest, template);
      return;
    }
    var planFactory = new PlanFactory(Main::mapping, force);
    executePlan(planFactory, dir, dest, privateDest, template);
  }
}
