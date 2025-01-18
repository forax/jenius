package com.github.jenius.component;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class XMLTest {
  private static void assertSameDocument(String expectedText, String actualText) throws ParserConfigurationException, IOException, SAXException {
    var diff = DiffBuilder.compare(expectedText)
        .withTest(actualText)
        .ignoreWhitespace()
        .build();
    if (diff.hasDifferences()) {
      AssertionFailureBuilder.assertionFailure().message("nodes are not equivalent").expected(expectedText).actual(actualText).buildAndThrow();
    }
  }

  @Test
  public void replaceNode() throws IOException, ParserConfigurationException, SAXException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <paragraph>
          This is a test
        </paragraph>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <p>
          This is a test
        </p>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "paragraph", (_, attrs, b) -> b.node("p", attrs));
    XML.transform(new StringReader(input), style, writer);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceNodeWithChildren() throws IOException, ParserConfigurationException, SAXException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <section>
          <paragraph>
            This is a test
          </paragraph>
        </section>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <div>
          <paragraph>
            This is a test
          </paragraph>
        </div>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "section", (_, attrs, b) -> b.node("div", attrs));
    XML.transform(new StringReader(input), style, writer);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceUsingAttributes() throws IOException, ParserConfigurationException, SAXException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <list style="ordered">
          <item>This is a test</item>
        </list>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <ol>
          <li>This is a test</li>
        </ol>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(Map.of(
        "list", (_, attrs, b) -> {
          var ordered = "ordered".equals(attrs.get("style"));
          b.node(ordered ? "ol" : "li");
        },
        "item", (_, _, b) -> b.node("li")
    ));
    XML.transform(new StringReader(input), style, writer);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void setAnAttribute() throws IOException, ParserConfigurationException, SAXException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <exercise>
          This is an exercise
        </exercise>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <exercise draft="true">
          This is an exercise
        </exercise>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.alwaysMatch(
        (name, _, b) -> b.node(name, "draft", "true"));
    XML.transform(new StringReader(input), style, writer);
    assertSameDocument(expected, writer.toString());
  }
}