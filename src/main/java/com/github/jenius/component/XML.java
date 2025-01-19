package com.github.jenius.component;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

public class XML {
  private static final class UncheckedSAXException extends RuntimeException {
    private final SAXException saxException;

    private UncheckedSAXException(SAXException saxException) {
      this.saxException = saxException;
    }
  }

  private static abstract class SaxNodeAdapter implements NodeBuilder {
    private final XMLFilterImpl impl;

    private SaxNodeAdapter(XMLFilterImpl impl) {
      this.impl = impl;
    }

    @Override
    public final NodeBuilder node(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) {
      Objects.requireNonNull(name);
      Objects.requireNonNull(map);
      Objects.requireNonNull(children);
      try {
        return saxEvent(name, map, children);
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
    }

    @Override
    public final NodeBuilder text(String text) {
      Objects.requireNonNull(text);
      try {
        impl.characters(text.toCharArray(), 0, text.length());
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
      return this;
    }

    abstract NodeBuilder saxEvent(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException;
  }

  private sealed interface Action {
    enum Ignore implements Action { INSTANCE }
    record Replace(String newName) implements Action {}
  }

  private static XMLFilterImpl filter(XMLReader xmlReader, ComponentStyle style) {
    record Replacement(String name, Action action) {}
    var replacementStack = new ArrayDeque<Replacement>();
    return new XMLFilterImpl(xmlReader) {
      private NodeBuilder rewritingNodeBuilder() {
        return new SaxNodeAdapter(this) {
          private boolean calledOnce;

          @Override
          public NodeBuilder saxEvent(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
            if (calledOnce) {
              throw new IllegalStateException("this builder has already called once");
            }
            calledOnce = true;
            var contentHandler = getContentHandler();
            contentHandler.startElement("", name, name, AttributesUtil.asAttributes(map));
            var replacement = replacementStack.pop();
            replacementStack.push(new Replacement(replacement.name, new Action.Replace(name)));
            children.accept(delegatingNodeBuilder());
            return this;
          }
        };
      }

      private NodeBuilder delegatingNodeBuilder() {
        return new SaxNodeAdapter(this) {
          @Override
          public NodeBuilder saxEvent(String name, Map<String, String> map, Consumer<? super NodeBuilder> children) throws SAXException {
            startElement("", name, name, AttributesUtil.asAttributes(map));
            children.accept(this);
            endElement("", name, name);
            return this;
          }
        };
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
        var componentOpt = style.lookup(localName);
        if (componentOpt.isPresent()) {
          var component = componentOpt.orElseThrow();
          replacementStack.push(new Replacement(localName, Action.Ignore.INSTANCE));
          try {
            component.render(localName, AttributesUtil.asMap(attrs), rewritingNodeBuilder());
          } catch (UncheckedSAXException e) {
            throw e.saxException;
          }
        } else {
          super.startElement(uri, localName, qName, attrs);
        }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
        var replacement = replacementStack.peek();
        if (replacement != null && localName.equals(replacement.name)) {
          replacementStack.pop();
          switch (replacement.action) {
            case Action.Ignore _ -> {}
            case Action.Replace(String newName) -> super.endElement("", newName, newName);
          }
        } else {
          super.endElement(uri, localName, qName);
        }
      }
    };
  }

  public static NodeBuilder include(NodeBuilder builder, Reader reader) {
    if (!(builder instanceof SaxNodeAdapter saxNodeAdapter)) {
      throw new IllegalArgumentException("not a well known builder");
    }
    var filter = saxNodeAdapter.impl;
    var parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    try {
      var parser = parserFactory.newSAXParser();
      parser.parse(new InputSource(reader), new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
          filter.startElement(uri, localName, qName, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
          filter.endElement(uri, localName, qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
          filter.characters(ch, start, length);
        }
      });
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    } catch (SAXException e) {
      throw new UncheckedSAXException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return builder;
  }

  public static void transform(Reader reader, ComponentStyle style, Writer writer) throws IOException {
    Objects.requireNonNull(reader);
    Objects.requireNonNull(style);
    Objects.requireNonNull(writer);
    var parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(true);
    try {
      var parser = parserFactory.newSAXParser();
      var xmlReader = parser.getXMLReader();
      var transformerFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
      var transformer = transformerFactory.newTransformer();
      //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      var source = new SAXSource(filter(xmlReader, style), new InputSource(reader));
      transformer.transform(source, new StreamResult(writer));
    } catch (SAXException | ParserConfigurationException | TransformerException e) {
      throw new IOException(e);
    }
  }
}
