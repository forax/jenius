package com.github.jenius.talc;

import com.github.jenius.component.Component;
import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public record Generator(Path root, DocumentManager manager, UnaryOperator<String> mapping, Node stylesheet) {
  public Generator {
    Objects.requireNonNull(root);
    Objects.requireNonNull(manager);
    Objects.requireNonNull(mapping);
    Objects.requireNonNull(stylesheet);
  }

  private static ComponentStyle textDecoration() {
    return ComponentStyle.rename(
        "css-link", "link",
        "item", "li",
        "link", "a",
        "bold", "b",
        "italic", "i",
        "underline", "u",
        "tt", "tt"
    );
  }

  private static ComponentStyle noAnswer() {
    return ComponentStyle.of("answer", Component.discard());
  }

  private static ComponentStyle defaultStyle() {
    return ComponentStyle.of(
        "exercise", Component.of((_, attrs, b) ->
            b.node("div", Map.of("class", "exercise"),c -> c
                .node("h3", c2 -> c2
                    .text(attrs.getOrDefault("title", "")))
            )
        ),
        "section", Component.of((_, attrs, b) ->
          b.node("div", Map.of("class", "section"), c -> c
              .node("h2", c2 -> c2
                  .text(attrs.getOrDefault("title", "")))
          )
        ),
        "paragraph", Component.of((_, attrs, b) ->
            b.node("div", Map.of("class", "paragraph"))
        ),
        "list", Component.of((_, attrs, b) ->
            b.node(attrs.containsKey("ordered") ? "ol" : "ul")),
        "image", Component.of((_, attrs, b) -> {
          var src = attrs.getOrDefault("src", "");
          var width = attrs.getOrDefault("width", "");
          var height = attrs.getOrDefault("height", "");
          var align = attrs.getOrDefault("align", "left");
          b.node("img", Map.of(
              "src", src,
              "width", width,
              "height", height,
              "style", "height:" + height + ";width:" + width + ";align:" + align + ";"));
        }),
        "code", Component.of((_, _, b) ->
            b.node("pre", "class", "code", "width", "100%")),
        "infos", Component.of((_, _, b) ->
            b.node("table", "style", "font-size:100%", "width", "100%")),
        "team", Component.of((_, _, b) ->
            b.collect((team, b2) -> {
              var leaders = team.elements().stream().filter(n -> n.name().equals("leader")).toList();
              var members = team.elements().stream().filter(n -> n.name().equals("member")).toList();
              System.err.println("leaders " + leaders);
              System.err.println("members " + members);
              b2.node("tr", c -> {
                c.node("td", "align", "left", "valign", "top", c2 -> {
                   c2.node("h3", "style","font-size:90%", c3 -> c3.text("Responsables"));
                   leaders.forEach(c2::include);
                });
                c.node("td", "align", "right", "valign", "top", c2 -> {
                   c2.node("h3", "style","font-size:90%", c3 -> c3.text("Chargés de TD"));
                   members.forEach(c2::include);
                });
              });
            })
        ),
        "leader", "member", Component.of((name, attrs, b) ->
            b.node("div", "class", name, c -> {
              c.text(attrs.getOrDefault("name", "???") + " -- ");
              var www = attrs.get("www");
              var mail = attrs.get("mail");
              if (www != null) {
                c.node("a", "href", www, c2 -> c2.text("www"));
              }
              c.text(" -- ");
              if (mail != null) {
                c.node("a", "href", mail, c2 -> c2.text("@"));
              }
            }))
    );
  }

  private Summary readFileSummary(Path path) {
    try {
      return manager.getMetadata(path).summary();
    } catch (IOException e) {
      return DocumentManager.defaultSummary(Optional.empty(), path);
    }
  }

  private ComponentStyle file(Path filePath) throws IOException {
    var metadata = manager.getMetadata(filePath);
    var document = metadata.document();
    var summary = metadata.summary();
    var infosOpt = metadata.infosOpt();
    var breadcrumb = new BreadCrumb(List.of("IR1", "2024-2025"), List.of("../index.html", "../../index.html"));
    return ComponentStyle.of(
        "insert-content", Component.of((_, _, b) -> {
          for(var node : document.getFirstElement().childNodes()) {
            b.include(node);
          }
        }),
        "insert-title-text", Component.of((_, _, b) -> b.text(summary.title())),
        "insert-infos", Component.of((_, _, b) -> infosOpt.ifPresent(b::include)),
        "insert-breadcrumb", Component.of((_, _, b) -> {
          b.node("span", "class", "bread-crumb", c -> {
            var titles = breadcrumb.titles();
            var hrefs = breadcrumb.hrefs();
            for(var i = 0; i < titles.size(); i++) {
              if (i != 0) {
                c.text(" :: ");
              }
              var title = titles.get(i);
              c.node("a", "href", hrefs.get(i), c2 -> c2.text(title));
            }
          });
        }),
        "tdref", Component.of((_, attrs, b) -> {
          var name = attrs.getOrDefault("name", "");
          var refPath = filePath.resolveSibling(name);
          var refSummary = readFileSummary(refPath);
          b.node("li", c -> {
            c.node("a", "href", mapping.apply(name), c2 ->
                c2.text(refSummary.title()));
            c.node("br");
            c.text(refSummary.exercises().stream().map(s -> "[" + s + "]").collect(Collectors.joining(" ")));
          });
        })
    );
  }

  public void generate(Path dirPath, Path destPath) throws IOException {
    Objects.requireNonNull(dirPath);
    Objects.requireNonNull(destPath);
    var style = ComponentStyle.anyMatch(
        noAnswer(),
        file(dirPath),
        defaultStyle(),
        textDecoration()
    );
    try(var writer = Files.newBufferedWriter(destPath)) {
      XML.transform(stylesheet, writer, XML.OutputKind.HTML, style);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
