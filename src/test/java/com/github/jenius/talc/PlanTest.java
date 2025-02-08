package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlanTest {
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
  public void diffStatusPlan() throws IOException, URISyntaxException {
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