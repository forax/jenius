package com.github.jenius.component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
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

  private static abstract class SaxNodeAdapater implements NodeBuilder {
    @Override
    public final NodeBuilder node(String name, Map<? extends String, ?> map, Consumer<? super NodeBuilder> children) {
      try {
        saxEvent(name, map, children);
      } catch (SAXException e) {
        throw new UncheckedSAXException(e);
      }
      return this;
    }

    abstract void saxEvent(String name, Map<? extends String, ?> map, Consumer<? super NodeBuilder> children) throws SAXException;
  }

  private static XMLFilterImpl filter(XMLReader xmlReader, ComponentStyle style) {
    record Replacement(String oldName, String newName) {}
    var replacementStack = new ArrayDeque<Replacement>();
    return new XMLFilterImpl(xmlReader) {
      private NodeBuilder rewritingNodeBuilder(String oldName) {
        return new SaxNodeAdapater() {
          @Override
          public void saxEvent(String name, Map<? extends String, ?> map, Consumer<? super NodeBuilder> children) throws SAXException {
            var contentHandler = getContentHandler();
            if (contentHandler != null) {
              contentHandler.startElement("", name, name, AttributesUtil.asAttributes(map));
            }
            children.accept(delegatingNodeBuilder());
            replacementStack.push(new Replacement(oldName, name));
          }
        };
      }

      private NodeBuilder delegatingNodeBuilder() {
        return new SaxNodeAdapater() {
          @Override
          public void saxEvent(String name, Map<? extends String, ?> map, Consumer<? super NodeBuilder> children) throws SAXException {
            startElement("", name, name, AttributesUtil.asAttributes(map));
            children.accept(this);
            endElement("", name, name);
          }
        };
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
        var componentOpt = style.lookup(localName);
        if (componentOpt.isPresent()) {
          var component = componentOpt.orElseThrow();
          try {
            component.render(localName, AttributesUtil.asMap(attrs), rewritingNodeBuilder(localName));
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
        if (replacement != null && localName.equals(replacement.oldName)) {
          replacementStack.pop();
          super.endElement("", replacement.newName, replacement.newName);
        } else {
          super.endElement(uri, localName, qName);
        }
      }
    };
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
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      var source = new SAXSource(filter(xmlReader, style), new InputSource(reader));
      transformer.transform(source, new StreamResult(writer));
    } catch (SAXException | ParserConfigurationException | TransformerException e) {
      throw new IOException(e);
    }
  }
}
