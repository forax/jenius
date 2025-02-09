package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BreadCrumbTest {
  @Test
  public void shouldCreateBreadCrumbWithValidInput() {
    var names = List.of("Home", "Products", "Laptops");
    var hrefs = List.of(
        Path.of("/"),
        Path.of("/products"),
        Path.of("/products/laptops")
    );
    var breadcrumb = new BreadCrumb(names, hrefs);

    assertAll(
        () -> assertEquals(names, breadcrumb.names()),
        () -> assertEquals(hrefs, breadcrumb.hrefs())
    );

  }

  @Test
  public void shouldCreateDefensiveCopyOfLists() {
    var names = new ArrayList<String>();
    names.add("Home");
    var hrefs = new ArrayList<Path>();
    hrefs.add(Path.of("/"));
    var breadcrumb = new BreadCrumb(names, hrefs);
    names.add("Products");
    hrefs.add(Path.of("/products"));

    assertAll(
        () -> assertEquals(1, breadcrumb.names().size()),
        () -> assertEquals(1, breadcrumb.hrefs().size()),
        () -> assertEquals("Home", breadcrumb.names().get(0)),
        () -> assertEquals(Path.of("/"), breadcrumb.hrefs().get(0))
    );
  }

  @Test
  public void shouldThrowExceptionWhenSizesDoNotMatch() {
    var names = List.of("Home", "Products");
    var hrefs = List.of(Path.of("/"));

    assertThrows(IllegalArgumentException.class, () -> new BreadCrumb(names, hrefs));
  }

  @Test
  public void shouldThrowExceptionForNullLists() {
    assertThrows(NullPointerException.class,
        () -> new BreadCrumb(null, List.of(Path.of("/"))));
    assertThrows(NullPointerException.class,
        () -> new BreadCrumb(List.of("Home"), null));
  }

  @Test
  public void shouldThrowExceptionForListsContainingNull() {
    var names = Arrays.asList("Home", null);
    var hrefs = List.of(Path.of("/"), Path.of("/products"));
    assertThrows(NullPointerException.class, () -> new BreadCrumb(names, hrefs));

    var names2 = Arrays.asList("Home", "Products");
    var hrefs2 = Arrays.asList(Path.of("/"), null);
    assertThrows(NullPointerException.class, () -> new BreadCrumb(names2, hrefs2));
  }

  @Test
  public void shouldFormatToStringCorrectly() {
    var names = List.of("Home", "Products", "Laptops");
    var hrefs = List.of(
        Path.of("/"),
        Path.of("/products"),
        Path.of("/products/laptops")
    );
    var breadcrumb = new BreadCrumb(names, hrefs);

    assertEquals("Home (/) :: Products (/products) :: Laptops (/products/laptops)", breadcrumb.toString());
  }

  @Test
  public void shouldHandleEmptyLists() {
    var breadcrumb = new BreadCrumb(List.of(), List.of());

    assertAll(
        () -> assertTrue(breadcrumb.names().isEmpty()),
        () -> assertTrue(breadcrumb.hrefs().isEmpty()),
        () -> assertEquals("", breadcrumb.toString())
    );
  }

  @Test
  public void shouldPreserveListOrder() {
    var names = List.of("First", "Second", "Third");
    var hrefs = List.of(
        Path.of("/1"),
        Path.of("/2"),
        Path.of("/3")
    );
    var breadcrumb = new BreadCrumb(names, hrefs);

    assertAll(
        () -> assertEquals("First", breadcrumb.names().get(0)),
        () -> assertEquals("Second", breadcrumb.names().get(1)),
        () -> assertEquals("Third", breadcrumb.names().get(2)),
        () -> assertEquals(Path.of("/1"), breadcrumb.hrefs().get(0)),
        () -> assertEquals(Path.of("/2"), breadcrumb.hrefs().get(1)),
        () -> assertEquals(Path.of("/3"), breadcrumb.hrefs().get(2))
    );
  }
}