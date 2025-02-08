package com.github.jenius.component;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CompactMapTest {
  @Test
  public void emptyMapShouldHaveZeroSize() {
    var map = CompactMap.of();

    assertEquals(0, map.size());
    assertTrue(map.isEmpty());
  }

  @Test
  public void singleEntryMapShouldHaveCorrectSize() {
    var map = CompactMap.of("one", 1);

    assertEquals(1, map.size());
    assertFalse(map.isEmpty());
  }

  @Test
  public void shouldNotAllowNullKeysInConstruction() {
    assertThrows(NullPointerException.class,
        () -> CompactMap.of(null, 1));
    assertThrows(NullPointerException.class,
        () -> CompactMap.of("one", 1, null, 2));
  }

  @Test
  public void shouldNotAllowNullValuesInConstruction() {
    assertThrows(NullPointerException.class,
        () -> CompactMap.of("one", null));
    assertThrows(NullPointerException.class,
        () -> CompactMap.of("one", 1, "two", null));
  }

  @Test
  public void shouldNotAllowNullInContainsKey() {
    var map = CompactMap.of("one", 1);

    assertThrows(NullPointerException.class, () -> map.containsKey(null));
  }

  @Test
  public void shouldNotAllowNullInGet() {
    var map = CompactMap.of("one", 1);

    assertThrows(NullPointerException.class, () -> map.get(null));
  }

  @Test
  public void shouldContainKey() {
    var map = CompactMap.of("one", 1, "two", 2);

    assertTrue(map.containsKey("one"));
    assertTrue(map.containsKey("two"));
    assertFalse(map.containsKey("three"));
  }

  @Test
  public void shouldReturnCorrectValues() {
    var map = CompactMap.of("one", 1, "two", 2);

    assertEquals(1, map.get("one"));
    assertEquals(2, map.get("two"));
    assertNull(map.get("three"));
  }

  @Test
  public void shouldReturnDefaultValue() {
    var map = CompactMap.of("one", 1);

    assertEquals(1, map.getOrDefault("one", 99));
    assertEquals(99, map.getOrDefault("missing", 99));
  }

  @Test
  public void shouldCreateMapWithFourEntries() {
    var map = CompactMap.of("one", 1, "two", 2, "three", 3, "four", 4);

    assertEquals(4, map.size());
    assertEquals(1, map.get("one"));
    assertEquals(2, map.get("two"));
    assertEquals(3, map.get("three"));
    assertEquals(4, map.get("four"));
  }

  @Test
  public void entriesShouldBeUnmodifiable() {
    var map = CompactMap.of("one", 1);
    var entries = map.entrySet();

    assertThrows(UnsupportedOperationException.class, () -> entries.add(Map.entry("two", 2)));
  }

  @Test
  public void keysShouldBeUnmodifiable() {
    var map = CompactMap.of("one", 1);
    var keys = map.keySet();

    assertThrows(UnsupportedOperationException.class, () -> keys.add("two"));
  }

  @Test
  public void valuesShouldBeUnmodifiable() {
    var map = CompactMap.of("one", 1);
    var values = map.values();

    assertThrows(UnsupportedOperationException.class, () -> values.add(2));
  }

  @Test
  public void shouldMaintainInsertionOrder() {
    var map = CompactMap.of("one", 1, "two", 2, "three", 3);
    var keys = map.keySet().toArray(String[]::new);

    assertEquals("one", keys[0]);
    assertEquals("two", keys[1]);
    assertEquals("three", keys[2]);
  }
}