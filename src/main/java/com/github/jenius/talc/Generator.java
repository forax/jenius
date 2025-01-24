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
    return ComponentStyle.of(Map.ofEntries(
        Map.entry("exercise", (_, attrs, b) ->
            b.node("div", Map.of("class", "exercise"),c -> c
                .node("h3", c2 -> c2
                    .text(attrs.getOrDefault("title", "")))
            )
        ),
        Map.entry("section", (_, attrs, b) ->
          b.node("div", Map.of("class", "section"), c -> c
              .node("h2", c2 -> c2
                  .text(attrs.getOrDefault("title", "")))
          )
        ),
        Map.entry("paragraph", (_, attrs, b) ->
            b.node("div", Map.of("class", "paragraph"))
        ),
        Map.entry("list", (_, attrs, b) ->
            b.node(attrs.containsKey("ordered") ? "ol" : "ul")),
        Map.entry("image", (_, attrs, b) -> {
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
        Map.entry("code", (_, _, b) ->
            b.node("pre", "class", "code", "width", "100%")),
        Map.entry("infos", (_, _, b) ->
            b.node("table", "style", "font-size:100%", "width", "100%")),
        Map.entry("team", (_, _, b) ->
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
        Map.entry("leader", (_, attrs, b) ->
            b.node("div", "class", "leader", c -> {
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
            })),
        Map.entry("member", (_, attrs, b) ->
            b.node("div", "class", "member", c -> {
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
    ));
  }

  private Summary readFileSummary(Path filePath) {
    try {
      return manager.getSummary(FileKind.FILE, filePath);
    } catch (IOException e) {  // create a fake summary
      return new Summary(Utils.removeExtension(filePath.getFileName().toString()), List.of());
    }
  }

  private ComponentStyle file(FileKind kind, Path dirPath) throws IOException {
    var document = manager.getDocument(kind, dirPath);
    var summary = manager.getSummary(kind, dirPath);
    var breadcrumb = new BreadCrumb(List.of("IR1", "2024-2025"), List.of("../index.html", "../../index.html"));
    return ComponentStyle.of(Map.of(
        "insert-content", (_, _, b) -> {
          for(var node : document.getFirstElement().childNodes()) {
            b.include(node);
          }
        },
        "title", Component.discard(),
        "insert-title", (_, _, b) -> b.node("title").text(summary.title()),
        "insert-title-text", (_, _, b) -> b.text(summary.title()),
        "insert-breadcrumb", (_, _, b) -> {
            var div = b.node("span", "class", "bread-crumb", c -> {
              var titles = breadcrumb.titles();
              var hrefs = breadcrumb.hrefs();
              for(var i = 0; i < titles.size(); i++) {
                if (i != 0) {
                  c.text(" :: ");
                }
                c.node("a", "href", hrefs.get(i))
                    .text(titles.get(i));
              }
            });
          },
        "tdref", (_, attrs, b) -> {
          var name = attrs.getOrDefault("name", "");
          var filePath = dirPath.resolve(name);
          var fileSummary = readFileSummary(filePath);
          b.node("li", c -> {
            c.node("a", "href", mapping.apply(name), c2 ->
                c2.text(fileSummary.title()));
            c.node("br");
            c.text(fileSummary.exercises().stream().map(s -> "[" + s + "]").collect(Collectors.joining(" ")));
          });
        }
    ));
  }

  public void generate(FileKind kind, Path dirPath, Path destPath) throws IOException {
    Objects.requireNonNull(kind);
    Objects.requireNonNull(dirPath);
    Objects.requireNonNull(destPath);
    var style = ComponentStyle.anyMatch(
        noAnswer(),
        file(kind, dirPath),
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
