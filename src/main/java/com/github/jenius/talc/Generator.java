package com.github.jenius.talc;

import com.github.jenius.component.Component;
import com.github.jenius.component.ComponentStyle;
import com.github.jenius.component.Node;
import com.github.jenius.component.XML;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class Generator {
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
        //<img src="{@src}" width="{@width}" style="height:{@height};width:{@width};align:{@align}"/>
        Map.entry("code", (_, _, b) ->
            b.node("pre", "class", "code", "width", "100%")))
    );
  }

  private static ComponentStyle file(Node document) {
    return ComponentStyle.of(Map.of(
        //"td",  (_, _, b) -> b.include(stylesheet.getFirstElement()),
        "insert-content", (_, _, b) -> {
          for(var node : document.getFirstElement().childNodes()) {
            b.include(node);
          }
        },
        "title", Component.discard()
    ));
  }

  public static void generate(Node document, Writer writer, Node stylesheet, Summary summary) throws IOException {
    ComponentStyle style = ComponentStyle.anyMatch(
        noAnswer(),
        file(document),
        defaultStyle(),
        textDecoration()
    );
    XML.transform(stylesheet, writer, XML.OutputKind.HTML, style);
  }
}
