package com.github.jenius.talc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class TalcGeneratorTest {
  @Test
  public void generateIdxForIndex() throws IOException, URISyntaxException {
    String title;
    try(var input = TalcGenerator.class.getResourceAsStream("index.xumlv");
        var reader = new InputStreamReader(input, UTF_8)) {
      title = TalcGenerator.extractTitleFromIndex(reader);
    }
    assertEquals("Programmation Objet avec Java", title);
  }

  @Test
  public void generateIdxForTd() throws IOException, URISyntaxException {
    TalcGenerator.Summary summary;
    try(var input = TalcGenerator.class.getResourceAsStream("td01.xumlv");
        var reader = new InputStreamReader(input, UTF_8)) {
      summary = TalcGenerator.extractSummary(reader);
    }
    System.out.println(summary);
    var expected = new TalcGenerator.Summary("Premiers pas en Java, chaînes de caractères, tableaux, boucles",
        List.of("Hello Groland", "Afficher les arguments de la ligne de commande", "Calculette simple", "Bien le bonjour", "De C vers Java"));
    assertEquals(expected, summary);
  }
}