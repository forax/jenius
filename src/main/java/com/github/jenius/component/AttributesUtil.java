package com.github.jenius.component;

import org.w3c.dom.NamedNodeMap;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

final class AttributesUtil {
  private AttributesUtil() {
    throw new AssertionError();
  }

  private static final class AttributeMap extends AbstractMap<String, String> {
    private final Attributes attributes;

    private AttributeMap(Attributes attributes) {
      this.attributes = attributes;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
      return new AbstractSet<>() {
        @Override
        public int size() {
          return attributes.getLength();
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
          return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
              return index < attributes.getLength();
            }

            @Override
            public Entry<String, String> next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              var key = attributes.getLocalName(index);
              var value = attributes.getValue(index);
              index++;
              return Map.entry(key, value);
            }
          };
        }
      };
    }

    @Override
    public int size() {
      return attributes.getLength();
    }

    @Override
    public boolean containsKey(Object key) {
      Objects.requireNonNull(key);
      return attributes.getIndex("", key.toString()) != -1;
    }

    @Override
    public String getOrDefault(Object key, String defaultValue) {
      Objects.requireNonNull(key);
      var index = attributes.getIndex("", key.toString());
      if (index == -1) {
        return defaultValue;
      }
      return attributes.getValue(index);
    }

    @Override
    public String get(Object key) {
      return getOrDefault(key, null);
    }
  }

  public static Map<String, String> asMap(Attributes attribute) {
    return new AttributeMap(attribute);
  }

  public static Attributes asAttributes(Map<String, String> map) {
    if (map instanceof AttributeMap attributeMap) {
      return attributeMap.attributes;
    }
    var attributes = new AttributesImpl();
    map.forEach((key, value) -> {
      attributes.addAttribute("", key, key, "CDATA", value);
    });
    return attributes;
  }

  public static Attributes asAttributes(NamedNodeMap namedNodeMap) {
    var attributes = new AttributesImpl();
    if (namedNodeMap == null) {
      return attributes;
    }
    for(var i = 0; i < attributes.getLength(); i++) {
      var item = namedNodeMap.item(i);
      var key = item.getNodeName();
      var value = item.getNodeValue();
      attributes.addAttribute("", key, key, "CDATA", value);
    }
    return attributes;
  }
}
