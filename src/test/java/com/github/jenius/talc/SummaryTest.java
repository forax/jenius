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

public class SummaryTest {
  @Test
  public void generateIndexSummary() throws IOException {
    Summary summary;
    try(var input = SummaryTest.class.getResourceAsStream("root/Java/index.xumlv");
        var reader = new InputStreamReader(input, UTF_8)) {
      var document = XML.transform(reader);
      summary = DocumentManager.extractSummary(document).orElseThrow();
    }
    assertEquals("Programmation Objet avec Java", summary.title());
  }

  @Test
  public void generateFileSummary() throws IOException {
    Summary summary;
    try(var input = SummaryTest.class.getResourceAsStream("root/Java/td01.xumlv");
        var reader = new InputStreamReader(input, UTF_8)) {
      var document = XML.transform(reader);
      summary = DocumentManager.extractSummary(document).orElseThrow();
    }
    var expected = new Summary("Premiers pas en Java, chaînes de caractères, tableaux, boucles",
        List.of("Hello Groland", "Afficher les arguments de la ligne de commande", "Calculette simple", "Bien le bonjour", "De C vers Java"));
    assertEquals(expected, summary);
  }

  // FIXME, need to be move somewhere else !
  @Test
  public void validateTemplate() throws IOException, URISyntaxException {
    var input = Files.readString(Path.of(SummaryTest.class.getResource("root/template.html").toURI()));
    //assertSameDocument(input, input);
    var style = ComponentStyle.alwaysMatch(Component.identity());
    var node = XML.transform(new StringReader(input), style);
    //System.out.println(node);

    //assertSameDocument(input, node.toString());  // FIXME
  }
}