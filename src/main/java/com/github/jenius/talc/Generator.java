package com.github.jenius.talc;

import com.github.jenius.component.Component;
import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static java.util.stream.Collectors.joining;

public record Generator(DocumentManager manager, UnaryOperator<String> mapping, Node template) {
  public Generator {
    Objects.requireNonNull(manager);
    Objects.requireNonNull(mapping);
    Objects.requireNonNull(template);
  }

  private static ComponentStyle answer(boolean activateAnswer) {
    Component component = activateAnswer
        ? (_, _, b) -> {
          b.node("div", "class", "answer", c ->
            c.around(
              pre -> pre
                .node("img",
                    "src", "http://igm.univ-mlv.fr/ens/resources/filaretordre.png",
                    "style", "align:center; width:80%")
                .node("br"),
            post -> post
                .node("br")
                .node("img",
                    "src", "http://igm.univ-mlv.fr/ens/resources/filaretordre2.png",
                    "style", "align:center; width:80%")
          ));
        }
        : (_, _, b) -> b.hide();
    return ComponentStyle.of("answer", component);
  }

  //
  // xmlv style is defined in tipi.dtd
  //
  private static ComponentStyle textDecoration() {
    return ComponentStyle.rename(
        "css-link", "link",

        "underline", "u",
        "bold", "b",
        //"sup", "sup",
        //"sub", "sub",
        "italic", "i",

        //"table", "table",
        "row", "tr",
        "tab", "td"

        //"br", "br"
    );
  }

  private static ComponentStyle defaultStyle() {
    return ComponentStyle.of(
        "link", Component.of((_, attrs, b) -> {
          var ref = attrs.getOrDefault("href", "");
          if (!ref.contains("://") && ref.endsWith(".php")) {
            ref = Utils.removeExtension(ref) + ".html";  // there is no PHP anymore
          }
          b.node("a", "href", ref);
        }),
        "section", Component.of((_, attrs, b) ->
          b.node("div", Map.of("class", "section"), c -> c
              .node("h2", c2 -> c2
                  .text(attrs.getOrDefault("title", "")))
          )
        ),
        "subsection", Component.of((_, attrs, b) ->
            b.node("div", Map.of("class", "subsection"), c -> c
                .node("h3", c2 -> c2
                    .text(attrs.getOrDefault("title", "")))
            )
        ),
        "abstract", "subtitle", Component.of((name, attrs, b) ->
          b.node("div", Map.of("class", name))
        ),
        "paragraph", Component.of((name, attrs, b) -> {
          var className =  attrs.getOrDefault("class", name);
          b.node("p", Map.of("class", className));
        }),
        "exercise", Component.of((_, attrs, b) ->
            b.node("div", Map.of("class", "exercise"),c -> c
                .node("h3", c2 -> c2
                    .text(attrs.getOrDefault("title", "Exercice")))
            )
        ),
        "list", Component.of((_, attrs, b) ->
            b.node("ordered".equals(attrs.get("style")) ? "ol" : "ul")
        ),
        "item", Component.of((name, attrs, b) -> {
          var className =  attrs.getOrDefault("class", "item");
          b.node("li", Map.of("class", className));
        }),

        "infos", Component.of((_, _, b) ->
            b.node("table", "style", "font-size:100%", "width", "100%")),
        "team", Component.of((_, _, b) ->
            b.collect((team, b2) -> {
              var leaders = team.elements().stream().filter(n -> n.name().equals("leader")).toList();
              var members = team.elements().stream().filter(n -> n.name().equals("member")).toList();
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
        "calendar", "projectref", "projectlink", "keydate", "photolink", "url", Component.discard(),  // not supported anymore
        "leader", "member", Component.of((name, attrs, b) ->
            b.node("div", "class", name, c -> {
              c.text(attrs.getOrDefault("name", "???") + " -- ");
              var www = attrs.get("www");
              var mail = attrs.get("mail");
              if (www != null && !www.isEmpty()) {
                c.node("a", "href", www, c2 -> c2.text("www"));
              }
              c.text(" -- ");
              if (mail != null && !mail.isEmpty()) {
                c.node("a", "href", "mailto:" + mail, c2 -> c2.text("@"));
              }
            })
        ),

        "code", Component.of((_, attrs, b) -> {
          var className =  attrs.getOrDefault("class", "code");
          b.node("pre", "class", className, "width", "100%");
        }),
        "tt", Component.of((_, attrs, b) ->
            b.node("span", Map.of("class", "tt", "style", "font-family: monospace;"))
        ),
        "font", Component.of((_, attrs, b) ->
            b.node("span", "style", "color:" + attrs.getOrDefault("color", "black"))
        ),
        "latex", "applet", Component.discard(),  // not supported anymore
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
        })
    );
  }

  private String breadcrumbHref(BreadCrumb breadCrumb, int index) {
    var size = breadCrumb.hrefs().size();
    return "../".repeat(size - 1 - index) + mapping.apply(breadCrumb.hrefs().get(index).getFileName().toString());
  }

  private String readContent(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException e) {
      return null;
    }
  }

  private static String localRefName(Path filePath, Metadata metadata) {
    var filename = metadata.path().getFileName().toString();
    return switch (metadata) {
      case Metadata.Dir _ -> filename;
      case Metadata.File file -> {
        if (filename.equals("index.xumlv")) {
          yield filePath.getParent().relativize(file.path()).toString();
        }
        yield filename;
      }
    };
  }

  private ComponentStyle file(Path filePath, boolean privateAccess) throws IOException {
    assert filePath.getFileName().toString().endsWith(".xumlv");
    var metadata = manager.getFileMetadata(filePath);
    var document = metadata.document();
    var summary = metadata.summary();
    var breadcrumb = manager.getBreadCrumb(filePath);
    var infosOpt = metadata.infosOpt();
    return ComponentStyle.of(
        "insert-content", Component.of((_, _, b) -> {
          var firstElement = document.getFirstElement().orElseThrow();
          var draft = Boolean.parseBoolean(firstElement.attributes().getOrDefault("draft", "false"));
          if (draft & !privateAccess) {
            b.node("div", "class", "draft", c -> c.text("A venir ... "));
            return;
          }
          firstElement.childNodes().forEach(b::include);
        }),
        "insert-title-text", Component.of((_, _, b) -> b.text(summary.title())),
        "insert-infos", Component.of((_, _, b) -> {
          infosOpt.ifPresent(node -> {
            b.node("div", "class", "infos", c -> {
              c.include(node)
               .node("hr");
            });
          });
        }),
        "insert-breadcrumb", Component.of((_, _, b) -> {
          b.node("span", "class", "bread-crumb", c -> {
            var names = breadcrumb.names();
            for(var i = 0; i < names.size(); i++) {
              c.text(" :: ");
              var title = names.get(i);
              var href = breadcrumbHref(breadcrumb, i);
              c.node("a", "href", href, c2 -> c2.text(title));
            }
            c.text(" :: ");
          });
        }),
        "tdref", "dir", Component.of((_, attrs, b) -> {
          var name = attrs.getOrDefault("name", "");
          var refPath = filePath.resolveSibling(name);
          var refMetadata = manager.getMetadata(refPath);
          var refName = localRefName(filePath, refMetadata);
          var refSummary = refMetadata.summary();
          b.node("li", c -> {
            c.node("a", "href", mapping.apply(refName), c2 ->
                c2.text(refSummary.title()));
            c.node("br");
            c.text(refSummary.subsections().stream().map(s -> "[" + s + "]").collect(joining(" ")));
          });
        }),
        "srcref", Component.of((_, attrs, b) -> {
          var link = attrs.get("link");
          var name = attrs.getOrDefault("name", "");
          var href = (link == null) ? name : link;
          if (link == null) {
            var content = readContent(filePath.resolveSibling(name));
            if (content != null) {
              b.node("pre", c -> c.text(content));
              return;
            }
          }
          b.node("div", "class", "noprint", c ->
            c.node("a", "href", mapping.apply(href), c2 ->
              c2.node("img", "class", "noprint", "src", "http://igm.univ-mlv.fr/ens/resources/file.png")
            )
          );
        })
    );
  }

  public void generate(Path dirPath, Path destPath, boolean privateAccess) throws IOException {
    Objects.requireNonNull(dirPath);
    Objects.requireNonNull(destPath);
    var style = ComponentStyle.anyMatch(
        answer(privateAccess),
        file(dirPath, privateAccess),
        defaultStyle(),
        textDecoration()
    );
    try(var writer = Files.newBufferedWriter(destPath)) {
      XML.transform(template, writer, XML.OutputKind.HTML, style);
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
