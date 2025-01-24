package com.github.jenius.talc;

import com.github.jenius.component.Component;
import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.XML;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class TalcGeneratorTest {
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
  public void generateTitleFromIndex() throws IOException {
    String title;
    try(var input = TalcGenerator.class.getResourceAsStream("index.xumlv");
        var reader = new InputStreamReader(input, UTF_8)) {
      title = TalcGenerator.extractTitleFromIndex(reader);
    }
    assertEquals("Programmation Objet avec Java", title);
  }

  @Test
  public void generateSummaryFromFile() throws IOException {
    TalcGenerator.Summary summary;
    try(var input = TalcGenerator.class.getResourceAsStream("td01.xumlv");
        var reader = new InputStreamReader(input, UTF_8)) {
      summary = TalcGenerator.extractSummary(reader);
    }
    var expected = new TalcGenerator.Summary("Premiers pas en Java, chaînes de caractères, tableaux, boucles",
        List.of("Hello Groland", "Afficher les arguments de la ligne de commande", "Calculette simple", "Bien le bonjour", "De C vers Java"));
    assertEquals(expected, summary);
  }

  @Test
  public void validateTemplate() throws IOException, URISyntaxException {
    var input = Files.readString(Path.of(TalcGenerator.class.getResource("template.html").toURI()));
    //assertSameDocument(input, input);
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var node = XML.transform(new StringReader(input), style);
    //System.out.println(node);

    //assertSameDocument(input, node.toString());  // FIXME
  }
}