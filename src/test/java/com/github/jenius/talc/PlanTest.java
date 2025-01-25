package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class PlanTest {
  @Test
  public void test() throws IOException, URISyntaxException {
    var path = Path.of(PlanTest.class.getResource(".").toURI());
    var planFactory = new PlanFactory(name -> {
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
    });
    var plan = planFactory.diff(path.resolve("root"), path.resolve("dest"));
    System.out.println(plan);
  }
}