package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static com.github.jenius.talc.Status.Kind.PUBLIC;
import static com.github.jenius.talc.Status.State.ADDED;
import static com.github.jenius.talc.Status.State.REMOVED;
import static com.github.jenius.talc.Status.State.UPDATED;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;

public class PlanTest {
  @Test
  public void shouldCreatePlanWithValidStatusMap() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    statusMap.put(srcPath2, new Status(ADDED, PUBLIC, destPath2));
    var plan = new Plan(statusMap);

    assertAll(
        () -> assertNotNull(plan),
        () -> assertEquals(2, plan.statusMap().size()),
        () -> assertTrue(plan.statusMap().containsKey(srcPath1)),
        () -> assertTrue(plan.statusMap().containsKey(srcPath2)),
        () -> assertEquals(UPDATED, plan.statusMap().get(srcPath1).state()),
        () -> assertEquals(ADDED, plan.statusMap().get(srcPath2).state()),
        () -> assertEquals(destPath1, plan.statusMap().get(srcPath1).destFile()),
        () -> assertEquals(destPath2, plan.statusMap().get(srcPath2).destFile())
    );
  }

  @Test
  public void shouldThrowExceptionWhenStatusMapIsNull() {
    assertThrows(NullPointerException.class, () -> new Plan(null));
  }

  @Test
  public void shouldMaintainInsertionOrderInToString() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    statusMap.put(srcPath2, new Status(REMOVED, PUBLIC, destPath2));
    var plan = new Plan(statusMap);

    var expected = srcPath1 + ": " + new Status(UPDATED, PUBLIC, destPath1) + "\n" +
        srcPath2 + ": " + new Status(REMOVED, PUBLIC, destPath2);
    assertEquals(expected, plan.toString());
  }

  @Test
  public void shouldReturnUnmodifiableStatusMap() {
    var srcPath = Path.of("/src/file");
    var destPath = Path.of("/dest/file");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath, new Status(ADDED, PUBLIC, destPath));
    var plan = new Plan(statusMap);

    assertThrows(UnsupportedOperationException.class,
        () -> plan.statusMap().put(
            Path.of("/src/new"),
            new Status(ADDED, PUBLIC, Path.of("/dest/new"))
        ));
  }

  @Test
  public void shouldCreateDefensiveCopyOfInputMap() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    statusMap.put(srcPath2, new Status(ADDED, PUBLIC, destPath2));
    var plan = new Plan(statusMap);
    statusMap.put(Path.of("/src/file3"), new Status(REMOVED, PUBLIC, Path.of("/dest/file3")));

    assertAll(
        () -> assertEquals(2, plan.statusMap().size()),
        () -> assertNotEquals(statusMap.size(), plan.statusMap().size())
    );
  }

  @Test
  public void shouldPreserveEqualityBasedOnContent() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");

    var statusMap1 = new LinkedHashMap<Path, Status>();
    statusMap1.put(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    statusMap1.put(srcPath2, new Status(ADDED, PUBLIC, destPath2));
    var plan1 = new Plan(statusMap1);

    var statusMap2 = new LinkedHashMap<Path, Status>();
    statusMap2.put(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    statusMap2.put(srcPath2, new Status(ADDED, PUBLIC, destPath2));
    var plan2 = new Plan(statusMap2);

    assertAll(
        () -> assertEquals(plan1, plan2),
        () -> assertEquals(plan1.hashCode(), plan2.hashCode())
    );
  }


  private static String mapping(String name) {
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
  }

  @Test
  public void shouldDiffStatusPlan() throws IOException, URISyntaxException {
    var path = Path.of(PlanTest.class.getResource(".").toURI());
    var planFactory = new PlanFactory(PlanTest::mapping);
    var root = path.resolve("root");
    var dest = path.resolve("dest");
    var plan = planFactory.diff(root, dest);

    record StatusPair(Path path, Status status) {}
    var pairs = List.of(
        new StatusPair(root.resolve("index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("index.html"))),
        new StatusPair(root.resolve("template.html"), new Status(ADDED, PUBLIC, dest.resolve("template.html"))),
        new StatusPair(root.resolve("Java"), new Status(ADDED, PUBLIC, dest.resolve("Java"))),
        new StatusPair(root.resolve("Java/index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("Java/index.html"))),
        new StatusPair(root.resolve("Java/td01.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("Java/td01.html"))),
        new StatusPair(root.resolve("System"), new Status(ADDED, PUBLIC, dest.resolve("System"))),
        new StatusPair(root.resolve("System/index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("System/index.html"))),
        new StatusPair(root.resolve("System/td02.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("System/td02.html"))),
        new StatusPair(root.resolve("System/PRIVATE"), new Status(ADDED, PUBLIC, dest.resolve("System/PRIVATE"))),
        new StatusPair(root.resolve("System/PRIVATE/td02-ls.h"), new Status(ADDED, PUBLIC, dest.resolve("System/PRIVATE/td02-ls.h"))),
        new StatusPair(root.resolve("System/PRIVATE/td02-ls.c"), new Status(ADDED, PUBLIC, dest.resolve("System/PRIVATE/td02-ls.c"))),
        new StatusPair(dest.resolve("should_be_removed.txt"), new Status(REMOVED, PUBLIC, dest.resolve("should_be_removed.txt"))));
    var expected = pairs.stream().collect(toMap(StatusPair::path, StatusPair::status));

    assertEquals(expected,plan.statusMap());
  }
}