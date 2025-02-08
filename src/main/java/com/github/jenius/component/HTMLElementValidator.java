package com.github.jenius.component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HTMLElementValidator {
  // see https://developer.mozilla.org/en-US/docs/Web/HTML/Element
  private static final Set<String> HTML_ELEMENTS = Set.of(
      // main root
      "html",
      // document metadata
      "base", "head", "link", "meta", "style", "title",
      // sectioning root
      "body",
      // content sectioning
      "address", "article", "aside", "footer", "header",
      "h1", "h2", "h3", "h4", "h5", "h6",
      "hgroup", "main", "nav", "section", "search",
      // text content
      "blockquote", "dd", "div", "dl", "dt", "figcaption", "figure",
      "hr", "li", "menu", "ol", "p", "pre", "ul",
      // inline text
      "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data",
      "dfn", "em", "i", "kbd", "mark", "q", "rp", "rt", "ruby",
      "s", "samp", "small", "span", "strong", "sub", "sup", "time",
      "u", "var", "wbr",
      // image and multimedia
      "area", "audio", "img", "map", "track", "video",
      // embedded
      "embed", "fencedframe", "iframe", "object", "picture", "source",
      // SVG and MathML
      "svg", "math",
      // scripting
      "canvas", "noscript", "script",
      // demarcating edits
      "del", "ins",
      // table content
      "caption", "col", "colgroup", "table", "tbody", "td",
      "tfoot", "th", "thead", "tr",
      // forms
      "button", "datalist", "fieldset", "form", "input", "label",
      "legend", "meter", "optgroup", "option", "output", "progress",
      "select", "textarea",
      // interactive
      "details", "dialog", "summary",
      // web components
      "slot", "template",
      // obsolete and deprecated
      "center", "tt"
      //"acronym", "big", "content", "dir", "font", "frame",
      //"frameset", "image", "marquee", "menuitem", "nobr", "noembed",
      //"noframes", "param", "plaintext", "rb", "rtc", "shadow",
      //"strike", "xmp"
  );

  private static boolean isInvalidHTMLElement(String elementName) {
    return !HTML_ELEMENTS.contains(elementName.toLowerCase(Locale.ROOT));
  }

  public static XML.ContentHandler validateHTMLElements(XML.ContentHandler delegate) {
    return new XML.ContentHandler() {
      @Override
      public void declaration(String version, String encoding) {
        delegate.declaration(version, encoding);
      }

      @Override
      public void startDocument() {
        delegate.startDocument();
      }

      @Override
      public void endDocument() {
        delegate.endDocument();
      }

      @Override
      public void startElement(String name, Map<String, String> attrs) {
        if (isInvalidHTMLElement(name)) {
          throw new IllegalStateException("invalid HTML element " + name);
        }
        delegate.startElement(name, attrs);
      }

      @Override
      public void endElement(String name) {
        if (isInvalidHTMLElement(name)) {
          throw new IllegalStateException("invalid HTML element " + name);
        }
        delegate.endElement(name);
      }

      @Override
      public void characters(String content) {
        delegate.characters(content);
      }
    };
  }
}
