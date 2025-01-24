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
import java.util.StringJoiner;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

record Plan(Map<Path, Status> statusMap) {

  @Override
  public String toString() {
    var joiner = new StringJoiner("\n");
    statusMap.forEach((path, status) -> {
      joiner.add(path + ": " + status);
    });
    return joiner.toString();
  }

  private record Scan(Set<String> names, List<String> directories) {}

  private static Scan scan(Path directory) throws IOException {
    try(var files = Files.list(directory)) {
      return files.collect(Collectors.teeing(
          Collectors.mapping(p -> p.getFileName().toString(), Collectors.toSet()),
          Collectors.filtering(Files::isDirectory, Collectors.mapping(p -> p.getFileName().toString(),Collectors.toList())),
          Scan::new));
    }
  }

  /*private record SHA1(byte[] hashs) {
    @Override
    public boolean equals(Object o) {
      return o instanceof SHA1 sha1 && Arrays.equals(hashs, sha1.hashs);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(hashs);
    }

    @Override
    public String toString() {
      var builder = new StringBuilder();
      for (var b : hashs) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    }
  }

  private static SHA1 computeSHA1(Path path) throws IOException {
    MessageDigest sha1Digest;
    try {
      sha1Digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
    var buffer = new byte[8192];
    try (var input = Files.newInputStream(path)) {
      int read;
      while ((read = input.read(buffer)) != -1) {
        sha1Digest.update(buffer, 0, read);
      }
    }

    return new SHA1(sha1Digest.digest());
  }*/

  public sealed interface Status {
    enum EnumStatus implements Status { ADDED, REMOVED }
    record Updated(Path destFile) implements Status {}
  }

  private static void diff(Path dir, Set<String> dirSet, Path dest, Set<String> destSet, UnaryOperator<String> mapping, Map<Path, Status> plan) throws IOException {
    var dirSetMapped = new HashSet<>();
    for(var name : dirSet) {
      var dirFile = dir.resolve(name);
      var destName = mapping.apply(name);
      dirSetMapped.add(destName);
      if (destSet.contains(destName)) {
        var destFile = dest.resolve(destName);
        var dirFileTime = Files.getLastModifiedTime(dirFile);
        var destFileTime = Files.getLastModifiedTime(destFile);
        if (destFileTime.compareTo(dirFileTime) < 0) {  // use computeSHA1 depending on the kind of file ??
          plan.put(dirFile, new Status.Updated(destFile));
        }
      } else {
        plan.put(dirFile, Status.EnumStatus.ADDED);
      }
    }
    for(var destName : destSet) {
      if (!dirSetMapped.contains(destName)) {
        var destPath = dest.resolve(destName);
        plan.put(destPath, Status.EnumStatus.REMOVED);
      }
    }
  }

  private static void scanDirectory(Path dir, Path dest, UnaryOperator<String> mapping, Map<Path, Status> plan) throws IOException {
    var dirScan = scan(dir);
    if (Files.isDirectory(dest)) {
      var destScan = scan(dest);
      diff(dir, dirScan.names, dest, destScan.names, mapping, plan);
    } else {
      diff(dir, dirScan.names, dest, Set.of(), mapping, plan);
    }
    for(var directory : dirScan.directories) {
      var newDir = dir.resolve(directory);
      var newDest = dest.resolve(directory);
      scanDirectory(newDir, newDest, mapping, plan);
    }
  }

  public static Plan diff(Path dir, Path dest, UnaryOperator<String> mapping) throws IOException {
    Objects.requireNonNull(dir);
    Objects.requireNonNull(dest);
    if (!Files.isDirectory(dir)) {
      throw new IOException(dir + " is not a directory");
    }
    if (!Files.isDirectory(dest)) {
      throw new IOException(dest + " is not a directory");
    }
    var plan = new LinkedHashMap<Path, Status>();
    scanDirectory(dir, dest, mapping, plan);
    return new Plan(plan);
  }
}
