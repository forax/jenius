package com.github.jenius;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
  @Test
  public void parseConfigWithThreePaths() {
    var args = new String[]{"source", "dest", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertFalse(config.force()),
        () -> assertFalse(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertNull(config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }

  @Test
  public void parseConfigWithFourPaths() {
    var args = new String[]{"source", "dest", "private", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertFalse(config.force()),
        () -> assertFalse(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertEquals(Path.of("private"), config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }

  @Test
  public void parseConfigWithThreePathsAndForce() {
    var args = new String[]{"--force", "source", "dest", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertTrue(config.force()),
        () -> assertFalse(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertNull(config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }

  @Test
  public void parseConfigWithThreePathsAndWatch() {
    var args = new String[]{"--watch", "source", "dest", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertFalse(config.force()),
        () -> assertTrue(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertNull(config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }

  @Test
  public void parseConfigWithFourPathsAndForce() {
    var args = new String[]{"--force", "source", "dest", "private", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertTrue(config.force()),
        () -> assertFalse(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertEquals(Path.of("private"), config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }

  @Test
  public void parseConfigWithFourPathsAndWatch() {
    var args = new String[]{"--watch", "source", "dest", "private", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertFalse(config.force()),
        () -> assertTrue(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertEquals(Path.of("private"), config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }

  @Test
  public void parseConfigWithForceAndWatchOptionInDifferentPositions() {
    var args = new String[]{"source", "--force", "dest", "--watch", "private", "template.html"};
    var config = Config.parseConfig(args);

    assertAll(
        () -> assertTrue(config.force()),
        () -> assertTrue(config.watch()),
        () -> assertEquals(Path.of("source"), config.dir()),
        () -> assertEquals(Path.of("dest"), config.dest()),
        () -> assertEquals(Path.of("private"), config.privateDest()),
        () -> assertEquals(Path.of("template.html"), config.template())
    );
  }
}