package com.github.jenius.component;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentStyleTest {
  private static final Component TEST_COMPONENT = (_, _, _) -> {};

  @Test
  public void ofNameAndComponentShouldCreateStyle() {
    var style = ComponentStyle.of("test", TEST_COMPONENT);

    assertTrue(style.lookup("test").isPresent());
    assertEquals(TEST_COMPONENT, style.lookup("test").orElseThrow());
    assertTrue(style.lookup("unknown").isEmpty());
  }

  @Test
  public void ofNameAndComponentShouldRejectNulls() {
    assertThrows(NullPointerException.class, () -> ComponentStyle.of(null, TEST_COMPONENT));
    assertThrows(NullPointerException.class, () -> ComponentStyle.of("test", null));
  }

  @Test
  public void ofVarargsShouldHandleSingleNameComponent() {
    var style = ComponentStyle.of("test", TEST_COMPONENT);

    assertTrue(style.lookup("test").isPresent());
    assertTrue(style.lookup("unknown").isEmpty());
  }

  @Test
  public void ofVarargsShouldHandleMultipleNamesForComponent() {
    var style = ComponentStyle.of("test1", "test2", TEST_COMPONENT);

    assertTrue(style.lookup("test1").isPresent());
    assertTrue(style.lookup("test2").isPresent());
    assertEquals(TEST_COMPONENT, style.lookup("test1").orElseThrow());
    assertEquals(TEST_COMPONENT, style.lookup("test2").orElseThrow());
  }

  @Test
  public void ofVarargsShouldRejectInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> ComponentStyle.of("test"));
    assertThrows(IllegalArgumentException.class, () -> ComponentStyle.of("test1", "test2"));
    assertThrows(IllegalArgumentException.class, () -> ComponentStyle.of("test", 42));
  }

  @Test
  public void ofVarargsShouldRejectNullArguments() {
    assertThrows(NullPointerException.class, () -> ComponentStyle.of((Object[]) null));
    assertThrows(NullPointerException.class, () -> ComponentStyle.of("test", null));
  }

  @Test
  public void ofMapShouldCreateValidStyle() {
    var style = ComponentStyle.of(Map.of("test", TEST_COMPONENT));

    assertTrue(style.lookup("test").isPresent());
    assertTrue(style.lookup("unknown").isEmpty());
  }

  @Test
  public void ofMapShouldRejectNull() {
    assertThrows(NullPointerException.class,
        () -> ComponentStyle.of((Map<String, Component>) null));
  }

  @Test
  public void renameShouldCreateValidMappings() {
    var style = ComponentStyle.rename("oldName", "newName");

    var component = style.lookup("oldName");
    assertTrue(component.isPresent());
    assertTrue(style.lookup("unknown").isEmpty());
  }

  @Test
  public void renameShouldRejectOddNumberOfArguments() {
    assertThrows(IllegalArgumentException.class, () -> ComponentStyle.rename("single"));
  }

  @Test
  public void renameShouldRejectNull() {
    assertThrows(NullPointerException.class, () -> ComponentStyle.rename((String[]) null));
    assertThrows(NullPointerException.class, () -> ComponentStyle.rename("foo", null));
  }

  @Test
  public void anyMatchShouldCombineStyles() {
    var style1 = ComponentStyle.of("test1", TEST_COMPONENT);
    var style2 = ComponentStyle.of("test2", TEST_COMPONENT);
    var combined = ComponentStyle.anyMatch(style1, style2);

    assertTrue(combined.lookup("test1").isPresent());
    assertTrue(combined.lookup("test2").isPresent());
    assertTrue(combined.lookup("unknown").isEmpty());
  }

  @Test
  public void anyMatchShouldReturnFirstMatch() {
    var component1 = (Component) (_, _, _) -> {};
    var component2 = (Component) (_, _, _) -> {};
    var style1 = ComponentStyle.of("test", component1);
    var style2 = ComponentStyle.of("test", component2);
    var combined = ComponentStyle.anyMatch(style1, style2);

    assertEquals(component1, combined.lookup("test").orElseThrow());
  }

  @Test
  public void anyMatchShouldRejectNull() {
    assertThrows(NullPointerException.class, () ->
        ComponentStyle.anyMatch((ComponentStyle[]) null)
    );
  }

  @Test
  public void alwaysMatchShouldReturnComponentForAnyName() {
    var style = ComponentStyle.alwaysMatch(TEST_COMPONENT);

    assertTrue(style.lookup("foo").isPresent());
    assertEquals(TEST_COMPONENT, style.lookup("bar").orElseThrow());
  }

  @Test
  public void alwaysMatchShouldRejectNull() {
    assertThrows(NullPointerException.class, () -> ComponentStyle.alwaysMatch(null));
  }

  @Test
  public void ignoreAllOthersShouldReturnDiscardComponent() {
    var style = ComponentStyle.of("test", TEST_COMPONENT).ignoreAllOthers();

    assertTrue(style.lookup("test").isPresent());
    assertTrue(style.lookup("unknown").isPresent());
    var discardComponent = style.lookup("unknown").get();
    assertSame(Component.discard(), discardComponent);
  }
}