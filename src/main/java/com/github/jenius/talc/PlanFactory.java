package com.github.jenius.talc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.github.jenius.talc.Status.Kind.PRIVATE;
import static com.github.jenius.talc.Status.Kind.PUBLIC;
import static com.github.jenius.talc.Status.State.ADDED;
import static com.github.jenius.talc.Status.State.REMOVED;
import static com.github.jenius.talc.Status.State.UPDATED;

public final class PlanFactory {
  private final UnaryOperator<String> mapping;

  public PlanFactory(UnaryOperator<String> mapping) {
    this.mapping = Objects.requireNonNull(mapping);
  }

  private record Scan(Set<String> names, List<String> directories) { }

  private static List<String> excludePRIVATEDirectories(Scan scan) {
    return scan.directories.stream()
        .filter(name -> !name.equals("PRIVATE"))
        .toList();
  }

  private static Scan scan(Path directory) throws IOException {
    try(var files = Files.list(directory)) {
      return files
          .filter(p -> {
            var filename = p.getFileName().toString();
            return !filename.startsWith(".") && !filename.equals("private"); // ignore hidden files and private directory
          })
          .collect(Collectors.teeing(
          Collectors.mapping(p -> p.getFileName().toString(), Collectors.toSet()),
          Collectors.filtering(Files::isDirectory, Collectors.mapping(p -> p.getFileName().toString(),Collectors.toList())),
          Scan::new));
    }
  }

  private void diff(Path dir, Set<String> dirSet, Path dest, Set<String> destSet, Status.Kind kind, Plan plan) throws IOException {
    var dirSetMapped = new HashSet<>();
    for(var name : dirSet) {
      var dirFile = dir.resolve(name);
      var destName = mapping.apply(name);
      var destFile = dest.resolve(destName);
      dirSetMapped.add(destName);
      if (destSet.contains(destName)) {
        var dirFileTime = Files.getLastModifiedTime(dirFile);
        var destFileTime = Files.getLastModifiedTime(destFile);
        if (destFileTime.compareTo(dirFileTime) < 0) {  // use computeSHA1 depending on the kind of file ??
          plan.add(dirFile, new Status(UPDATED, kind, destFile));
        }
      } else {
        plan.add(dirFile, new Status(ADDED, kind, destFile));
      }
    }
    for(var destName : destSet) {
      if (!dirSetMapped.contains(destName)) {
        var destPath = dest.resolve(destName);
        plan.add(destPath, new Status(REMOVED, kind, destPath));
      }
    }
  }

  private void diffNames(Path dir, Scan dirScan, Path dest, Status.Kind kind, Plan plan) throws IOException {
    if (Files.isDirectory(dest)) {
      var destScan = scan(dest);
      diff(dir, dirScan.names, dest, destScan.names, kind, plan);
    } else {
      diff(dir, dirScan.names, dest, Set.of(), kind, plan);
    }
  }

  private void scanDirectory(Path dir, Path dest, Path privateDest, Plan plan) throws IOException {
    var dirScan = scan(dir);
    if (dest != null) {
      diffNames(dir, dirScan, dest, PUBLIC, plan);
    }
    if (privateDest != null) {
      diffNames(dir, dirScan, privateDest, PRIVATE, plan);
    }
    for(var directory : dirScan.directories) {
      var newDir = dir.resolve(directory);
      var newDest = dest == null || directory.equals("PRIVATE") ? null : dest.resolve(directory);
      var newPrivateDest = privateDest == null ? null : privateDest.resolve(directory);
      scanDirectory(newDir, newDest, newPrivateDest, plan);
    }
  }

  public Plan diff(Path dir, Path dest, Path privateDest) throws IOException {
    Objects.requireNonNull(dir);
    Objects.requireNonNull(dest);
    if (!Files.isDirectory(dir)) {
      throw new IOException(dir + " is not a directory");
    }
    if (!Files.isDirectory(dest)) {
      throw new IOException(dest + " is not a directory");
    }
    if (privateDest != null && !Files.isDirectory(privateDest)) {
      throw new IOException(privateDest + " is not a directory");
    }
    var plan = new Plan();
    scanDirectory(dir, dest, privateDest, plan);
    return plan;
  }
}
