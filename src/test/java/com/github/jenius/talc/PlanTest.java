package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PlanTest {
  @Test
  public void shouldCreatePlanWithValidStatusMap() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath1, new Status(Status.State.UPDATED, destPath1));
    statusMap.put(srcPath2, new Status(Status.State.ADDED, destPath2));
    var plan = new Plan(statusMap);

    assertAll(
        () -> assertNotNull(plan),
        () -> assertEquals(2, plan.statusMap().size()),
        () -> assertTrue(plan.statusMap().containsKey(srcPath1)),
        () -> assertTrue(plan.statusMap().containsKey(srcPath2)),
        () -> assertEquals(Status.State.UPDATED, plan.statusMap().get(srcPath1).state()),
        () -> assertEquals(Status.State.ADDED, plan.statusMap().get(srcPath2).state()),
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
    statusMap.put(srcPath1, new Status(Status.State.UPDATED, destPath1));
    statusMap.put(srcPath2, new Status(Status.State.REMOVED, destPath2));
    var plan = new Plan(statusMap);

    var expected = srcPath1 + ": " + new Status(Status.State.UPDATED, destPath1) + "\n" +
        srcPath2 + ": " + new Status(Status.State.REMOVED, destPath2);
    assertEquals(expected, plan.toString());
  }

  @Test
  public void shouldReturnUnmodifiableStatusMap() {
    var srcPath = Path.of("/src/file");
    var destPath = Path.of("/dest/file");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath, new Status(Status.State.ADDED, destPath));
    var plan = new Plan(statusMap);

    assertThrows(UnsupportedOperationException.class,
        () -> plan.statusMap().put(
            Path.of("/src/new"),
            new Status(Status.State.ADDED, Path.of("/dest/new"))
        ));
  }

  @Test
  public void shouldCreateDefensiveCopyOfInputMap() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var statusMap = new LinkedHashMap<Path, Status>();
    statusMap.put(srcPath1, new Status(Status.State.UPDATED, destPath1));
    statusMap.put(srcPath2, new Status(Status.State.ADDED, destPath2));
    var plan = new Plan(statusMap);
    statusMap.put(Path.of("/src/file3"), new Status(Status.State.REMOVED, Path.of("/dest/file3")));

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
    statusMap1.put(srcPath1, new Status(Status.State.UPDATED, destPath1));
    statusMap1.put(srcPath2, new Status(Status.State.ADDED, destPath2));
    var plan1 = new Plan(statusMap1);

    var statusMap2 = new LinkedHashMap<Path, Status>();
    statusMap2.put(srcPath1, new Status(Status.State.UPDATED, destPath1));
    statusMap2.put(srcPath2, new Status(Status.State.ADDED, destPath2));
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

    var statusMap = plan.statusMap();
    assertEquals(Map.of(
        root.resolve("index.xumlv"), new Status(Status.State.ADDED, dest.resolve("index.html")),
        root.resolve("template.html"), new Status(Status.State.ADDED, dest.resolve("template.html")),
        root.resolve("Java"), new Status(Status.State.ADDED, dest.resolve("Java")),
        root.resolve("Java/index.xumlv"), new Status(Status.State.ADDED, dest.resolve("Java/index.html")),
        root.resolve("Java/td01.xumlv"), new Status(Status.State.ADDED, dest.resolve("Java/td01.html")),
        dest.resolve("should_be_removed.txt"), new Status(Status.State.REMOVED, dest.resolve("should_be_removed.txt"))
    ), statusMap);
  }
}