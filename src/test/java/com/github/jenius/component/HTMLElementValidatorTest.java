package com.github.jenius.component;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HTMLElementValidatorTest {
  private static final class TestContentHandler implements XML.ContentHandler {
    private boolean documentStarted = false;
    private boolean documentEnded = false;
    private String lastDeclarationVersion;
    private String lastDeclarationEncoding;
    private String lastStartElement;
    private String lastEndElement;
    private Map<String, String> lastAttributes;
    private String lastCharacters;

    @Override
    public void declaration(String version, String encoding) {
      lastDeclarationVersion = version;
      lastDeclarationEncoding = encoding;
    }

    @Override
    public void startDocument() {
      documentStarted = true;
    }

    @Override
    public void endDocument() {
      documentEnded = true;
    }

    @Override
    public void startElement(String name, Map<String, String> attrs) {
      lastStartElement = name;
      lastAttributes = attrs;
    }

    @Override
    public void endElement(String name) {
      lastEndElement = name;
    }

    @Override
    public void characters(String content) {
      lastCharacters = content;
    }
  }

  @Test
  public void shouldHandleValidElement() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    var attrs = Map.of("class", "test-class");
    validatedHandler.startElement("div", attrs);
    assertEquals("div", testHandler.lastStartElement);
    assertEquals(attrs, testHandler.lastAttributes);

    validatedHandler.endElement("div");
    assertEquals("div", testHandler.lastEndElement);
  }

  @Test
  public void shouldHandleUppercaseValidElement() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    validatedHandler.startElement("DIV", Map.of());
    assertEquals("DIV", testHandler.lastStartElement);

    validatedHandler.endElement("DIV");
    assertEquals("DIV", testHandler.lastEndElement);
  }

  @Test
  public void shouldRejectInvalidElement() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    assertThrows(IllegalStateException.class,
        () -> validatedHandler.startElement("invalid-tag", Map.of()));

    assertThrows(IllegalStateException.class,
        () -> validatedHandler.endElement("invalid-tag"));
  }

  @Test
  public void shouldPassThroughDocumentEvents() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    validatedHandler.startDocument();
    assertTrue(testHandler.documentStarted);

    validatedHandler.endDocument();
    assertTrue(testHandler.documentEnded);
  }

  @Test
  public void shouldPassThroughDeclaration() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    validatedHandler.declaration("1.0", "UTF-8");
    assertEquals("1.0", testHandler.lastDeclarationVersion);
    assertEquals("UTF-8", testHandler.lastDeclarationEncoding);
  }

  @Test
  public void shouldPassThroughCharacters() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    var content = "Test content";
    validatedHandler.characters(content);
    assertEquals(content, testHandler.lastCharacters);
  }

  @Test
  public void shouldValidateCompleteDocument() {
    var testHandler = new TestContentHandler();
    var validatedHandler = HTMLElementValidator.validateHTMLElements(testHandler);

    validatedHandler.declaration("1.0", "UTF-8");
    validatedHandler.startDocument();
    validatedHandler.startElement("html", Map.of());
    validatedHandler.startElement("body", Map.of());
    validatedHandler.startElement("div", Map.of());
    validatedHandler.characters("Test content");
    validatedHandler.endElement("div");
    validatedHandler.endElement("body");
    validatedHandler.endElement("html");
    validatedHandler.endDocument();

    assertTrue(testHandler.documentStarted);
    assertTrue(testHandler.documentEnded);
    assertEquals("Test content", testHandler.lastCharacters);
  }
}