package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static com.github.jenius.talc.Status.Kind.PRIVATE;
import static com.github.jenius.talc.Status.Kind.PUBLIC;
import static com.github.jenius.talc.Status.State.ADDED;
import static com.github.jenius.talc.Status.State.REMOVED;
import static com.github.jenius.talc.Status.State.UPDATED;
import static org.junit.jupiter.api.Assertions.*;

public class PlanTest {
  @Test
  public void shouldCreatePlanWithValidStatusMap() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var plan = new Plan();
    plan.add(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    plan.add(srcPath2, new Status(ADDED, PUBLIC, destPath2));

    assertAll(
        () -> assertNotNull(plan),
        () -> assertEquals(2, plan.statusMap().size()),
        () -> assertTrue(plan.statusMap().containsKey(srcPath1)),
        () -> assertTrue(plan.statusMap().containsKey(srcPath2)),
        () -> assertEquals(UPDATED, plan.statusMap().get(srcPath1).getFirst().state()),
        () -> assertEquals(ADDED, plan.statusMap().get(srcPath2).getFirst().state()),
        () -> assertEquals(destPath1, plan.statusMap().get(srcPath1).getFirst().destFile()),
        () -> assertEquals(destPath2, plan.statusMap().get(srcPath2).getFirst().destFile())
    );
  }

  @Test
  public void shouldAddAndRemovePathMutatePlan() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var plan = new Plan();
    plan.add(srcPath1, new Status(ADDED, PUBLIC, srcPath1));
    plan.add(srcPath2, new Status(ADDED, PUBLIC, srcPath2));
    plan.remove(srcPath1);

    assertAll(
        () -> assertEquals(1, plan.statusMap().size()),
        () -> assertFalse(plan.statusMap().containsKey(srcPath1)),
        () -> assertTrue(plan.statusMap().containsKey(srcPath2))
    );
  }

  @Test
  public void shouldMaintainInsertionOrderInToString() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");
    var plan = new Plan();
    plan.add(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    plan.add(srcPath2, new Status(REMOVED, PUBLIC, destPath2));

    var expected = srcPath1 + ": " + new Status(UPDATED, PUBLIC, destPath1) + "\n" +
        srcPath2 + ": " + new Status(REMOVED, PUBLIC, destPath2);
    assertEquals(expected, plan.toString());
  }

  @Test
  public void shouldReturnUnmodifiableStatusMap() {
    var srcPath = Path.of("/src/file");
    var destPath = Path.of("/dest/file");
    var plan = new Plan();
    plan.add(srcPath, new Status(ADDED, PUBLIC, destPath));

    assertThrows(UnsupportedOperationException.class,
        () -> plan.statusMap().put(
            Path.of("/src/new"),
            List.of(new Status(ADDED, PUBLIC, Path.of("/dest/new")))
        ));
  }

  @Test
  public void shouldPreserveEqualityBasedOnContent() {
    var srcPath1 = Path.of("/src/file1");
    var srcPath2 = Path.of("/src/file2");
    var destPath1 = Path.of("/dest/file1");
    var destPath2 = Path.of("/dest/file2");

    var plan1 = new Plan();
    plan1.add(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    plan1.add(srcPath2, new Status(ADDED, PUBLIC, destPath2));

    var plan2 = new Plan();
    plan2.add(srcPath1, new Status(UPDATED, PUBLIC, destPath1));
    plan2.add(srcPath2, new Status(ADDED, PUBLIC, destPath2));

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
    var resource = PlanTest.class.getResource(".");
    assert resource != null;
    var path = Path.of(resource.toURI());
    var planFactory = new PlanFactory(PlanTest::mapping, false);
    var root = path.resolve("root");
    var dest = path.resolve("dest");
    var plan = planFactory.diff(root, dest, null);

    var plan2 = new Plan();
    plan2.add(root.resolve("index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("index.html")));
    plan2.add(root.resolve("Java"), new Status(ADDED, PUBLIC, dest.resolve("Java")));
    plan2.add(root.resolve("Java/index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("Java/index.html")));
    plan2.add(root.resolve("Java/td01.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("Java/td01.html")));
    plan2.add(root.resolve("System"), new Status(ADDED, PUBLIC, dest.resolve("System")));
    plan2.add(root.resolve("System/PRIVATE"), new Status(ADDED, PUBLIC, dest.resolve("System/PRIVATE")));
    plan2.add(root.resolve("System/index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("System/index.html")));
    plan2.add(root.resolve("System/td02.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("System/td02.html")));
    plan2.add(root.resolve("System/projet.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("System/projet.html")));
    plan2.add(dest.resolve("should_be_removed.txt"), new Status(REMOVED, PUBLIC, dest.resolve("should_be_removed.txt")));

    assertEquals(plan2,plan);
  }

  @Test
  public void shouldDiffWithPrivateStatusPlan() throws IOException, URISyntaxException {
    var resource = PlanTest.class.getResource(".");
    assert resource != null;
    var path = Path.of(resource.toURI());
    var planFactory = new PlanFactory(PlanTest::mapping, false);
    var root = path.resolve("root/System");
    var dest = path.resolve("dest");
    var privateDest = dest.resolve("private");
    var plan = planFactory.diff(root, dest, privateDest);

    var plan2 = new Plan();
    plan2.add(root.resolve("index.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("index.html")));
    plan2.add(root.resolve("index.xumlv"), new Status(ADDED, PRIVATE, dest.resolve("private/index.html")));
    plan2.add(root.resolve("td02.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("td02.html")));
    plan2.add(root.resolve("td02.xumlv"), new Status(ADDED, PRIVATE, dest.resolve("private/td02.html")));
    plan2.add(root.resolve("projet.xumlv"), new Status(ADDED, PUBLIC, dest.resolve("projet.html")));
    plan2.add(root.resolve("projet.xumlv"), new Status(ADDED, PRIVATE, dest.resolve("private/projet.html")));
    plan2.add(root.resolve("PRIVATE"), new Status(ADDED, PUBLIC, dest.resolve("PRIVATE")));
    plan2.add(root.resolve("PRIVATE"), new Status(ADDED, PRIVATE, dest.resolve("private/PRIVATE")));
    plan2.add(root.resolve("PRIVATE/td02-ls.h"), new Status(ADDED, PRIVATE, dest.resolve("private/PRIVATE/td02-ls.h")));
    plan2.add(root.resolve("PRIVATE/td02-ls.c"), new Status(ADDED, PRIVATE, dest.resolve("private/PRIVATE/td02-ls.c")));
    plan2.add(dest.resolve("should_be_removed.txt"), new Status(REMOVED, PUBLIC, dest.resolve("should_be_removed.txt")));
    plan2.add(dest.resolve("private/should_be_removed2.txt"), new Status(REMOVED, PRIVATE, dest.resolve("private/should_be_removed2.txt")));

    assertEquals(plan2,plan);
  }
}