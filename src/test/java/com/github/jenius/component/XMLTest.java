package com.github.jenius.component;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Test;

import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class XMLTest {
  private static void assertSameDocument(String expectedText, String actualText) {
    var diff = DiffBuilder.compare(expectedText)
        .withTest(actualText)
        .ignoreWhitespace()
        .normalizeWhitespace()
        .ignoreElementContentWhitespace()
        .build();
    if (diff.hasDifferences()) {
      AssertionFailureBuilder.assertionFailure().message("nodes are not equivalent").expected(expectedText).actual(actualText).buildAndThrow();
    }
  }

  @Test
  public void replaceNode() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <paragraph foo="bar">
          This is a test
        </paragraph>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <p foo="bar">
          This is a test
        </p>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "paragraph", (_, attrs, b) -> b.node("p", attrs));
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceNodeWithChildren() throws IOException {
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
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceUsingAttributes() throws IOException {
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
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceNodeByANodeAndAText() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
        </foo>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bar>
          This is a test
        </bar>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "foo", (_, _, b) -> {
          b.node("bar")
              .text("This is a test");
        }
    );
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceNodeByAText() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
         <bar>
         </bar>
        </foo>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          This is a test
        </foo>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "bar", (_, _, b) -> b.text("This is a test")
    );
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML,style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replaceANodeIgnoreAllTheOthers() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <section>
          An ignored text.
          <paragraph>
            This is a test
          </paragraph>
          <ignored-tag>
            An another ignored text
          </ignored-tag>
        </section>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          This is a test
        </foo>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "paragraph", (_, attrs, b) -> b.node("foo", attrs))
        .ignoreAllOthers();
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void omitANode() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
         <bar>
          <baz>
          </baz>
         </bar>
        </foo>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <baz>
          </baz>
        </foo>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "bar", (name, attributes, nodeBuilder) -> {});
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void omitANodeWithText() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
         <bar>
          This should be removed
          <baz>
           This is a test
          </baz>
         </bar>
        </foo>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <baz>
            This is a test
          </baz>
        </foo>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of(
        "bar", (name, attributes, nodeBuilder) -> {});
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void setAnAttribute() throws IOException {
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
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void include() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <include>
          </include>
        </foo>
        """;
    var input2 = """
        <?xml version="1.0" encoding="UTF-8"?>
        <baz>
          This is a text
        </baz>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar>
            <baz>
              This is a text
            </baz>
          </bar>
        </foo>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of("include",
        (_, attrs, b) -> b.node("bar", attrs,
              children -> children.include(new StringReader(input2))));
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replayIdentity() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar>
            This is a text
          </bar>
        </foo>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <whizz>
          <bar>
            This is a text
          </bar>
        </whizz>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of("foo",
        (_, attrs, b) -> b.replay(n -> n.createNode("whizz", n.childNodes())));
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void replayIgnore() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar>
            This is a text
          </bar>
        </foo>
        """;
    var expected = """
        <?xml version="1.0" encoding="UTF-8"?>
        <whizz>
        </whizz>
        """;
    var writer = new StringWriter();
    var style = ComponentStyle.of("foo",
        (name, attrs, b) -> b.replay(n -> n.createNode("whizz")));
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(expected, writer.toString());
  }

  @Test
  public void transformReaderWriterIdentity() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var writer = new StringWriter();
    XML.transform(new StringReader(input), writer, XML.OutputKind.XML, style);
    assertSameDocument(input, writer.toString());
  }

  @Test
  public void transformToNodeIdentity() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var node = XML.transform(new StringReader(input), style);
    var writer = new StringWriter();
    XML.transform(node, writer, XML.OutputKind.XML, style);
    assertSameDocument(input, writer.toString());
  }

  @Test
  public void transformNodeIdentity() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var node = XML.transform(new StringReader(input));
    assertSameDocument(input, node.toString());
  }

  @Test
  public void transformNodeToNodeStyleIdentity() throws IOException {
    var input = """
        <?xml version="1.0" encoding="UTF-8"?>
        <foo>
          <bar glut="true">
            This is a text
          </bar>
        </foo>
        """;
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var node = XML.transform(new StringReader(input), style);
    var node2 = XML.transform(node, style);
    var writer = new StringWriter();
    XML.transform(node2, writer, XML.OutputKind.XML, style);
    assertSameDocument(input, writer.toString());
  }
}