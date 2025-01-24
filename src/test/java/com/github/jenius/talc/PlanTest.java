package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PlanTest {
  @Test
  public void test() throws IOException, URISyntaxException {
    var path = Path.of(PlanTest.class.getResource(".").toURI());
    var plan = Plan.diff(path.resolve("root"), path.resolve("dest"), name -> {
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
    System.out.println(plan);
  }
}