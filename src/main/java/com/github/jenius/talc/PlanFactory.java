package com.github.jenius.talc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class PlanFactory {
  private final UnaryOperator<String> mapping;

  public PlanFactory(UnaryOperator<String> mapping) {
    this.mapping = Objects.requireNonNull(mapping);
  }

  private record Scan(Set<String> names, List<String> directories) {}

  private static Scan scan(Path directory) throws IOException {
    try(var files = Files.list(directory)) {
      return files
          .filter(Predicate.not(p -> p.getFileName().toString().startsWith(".")))  // ignore hidden files
          .collect(Collectors.teeing(
          Collectors.mapping(p -> p.getFileName().toString(), Collectors.toSet()),
          Collectors.filtering(Files::isDirectory, Collectors.mapping(p -> p.getFileName().toString(),Collectors.toList())),
          Scan::new));
    }
  }

  private void diff(Path dir, Set<String> dirSet, Path dest, Set<String> destSet, Map<Path, Status> statusMap) throws IOException {
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
          statusMap.put(dirFile, new Status(Status.State.UPDATED, destFile));
        }
      } else {
        statusMap.put(dirFile, new Status(Status.State.ADDED, destFile));
      }
    }
    for(var destName : destSet) {
      if (!dirSetMapped.contains(destName)) {
        var destPath = dest.resolve(destName);
        statusMap.put(destPath, new Status(Status.State.REMOVED, destPath));
      }
    }
  }

  private void scanDirectory(Path dir, Path dest, Map<Path, Status> statusMap) throws IOException {
    var dirScan = scan(dir);
    if (Files.isDirectory(dest)) {
      var destScan = scan(dest);
      diff(dir, dirScan.names, dest, destScan.names, statusMap);
    } else {
      diff(dir, dirScan.names, dest, Set.of(), statusMap);
    }
    for(var directory : dirScan.directories) {
      var newDir = dir.resolve(directory);
      var newDest = dest.resolve(directory);
      scanDirectory(newDir, newDest, statusMap);
    }
  }

  public Plan diff(Path dir, Path dest) throws IOException {
    Objects.requireNonNull(dir);
    Objects.requireNonNull(dest);
    if (!Files.isDirectory(dir)) {
      throw new IOException(dir + " is not a directory");
    }
    if (!Files.isDirectory(dest)) {
      throw new IOException(dest + " is not a directory");
    }
    var statusMap = new LinkedHashMap<Path, Status>();
    scanDirectory(dir, dest, statusMap);
    return new Plan(statusMap);
  }
}
