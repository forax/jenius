package com.github.jenius.component;

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

  private static Object asValue(String s) {
    return switch (s) {
      case "true", "false" -> Boolean.valueOf(s);
      default -> {
        try {
          yield Double.parseDouble(s);
        } catch (NumberFormatException _) {
          try {
            yield Integer.parseInt(s);
          } catch (NumberFormatException _) {
            yield s;
          }
        }
      }
    };
  }

  private static final class AttributeMap extends AbstractMap<String, Object> {
    private final Attributes attributes;

    private AttributeMap(Attributes attributes) {
      this.attributes = attributes;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      return new AbstractSet<>() {
        @Override
        public int size() {
          return attributes.getLength();
        }

        @Override
        public Iterator<Entry<String, Object>> iterator() {
          return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
              return index < attributes.getLength();
            }

            @Override
            public Entry<String, Object> next() {
              if (!hasNext()) {
                throw new NoSuchElementException();
              }
              var key = attributes.getLocalName(index);
              var value = asValue(attributes.getValue(index));
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
      return attributes.getIndex(null, key.toString()) != -1;
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
      Objects.requireNonNull(key);
      var index = attributes.getIndex(null, key.toString());
      if (index == -1) {
        return defaultValue;
      }
      return asValue(attributes.getValue(index));
    }

    @Override
    public Object get(Object key) {
      return getOrDefault(key, null);
    }
  }

  public static Map<String, Object> asMap(Attributes attribute) {
    return new AttributeMap(attribute);
  }

  public static Attributes asAttributes(Map<? extends String, ?> map) {
    if (map instanceof AttributeMap attributeMap) {
      return attributeMap.attributes;
    }
    var attributes = new AttributesImpl();
    map.forEach((key, value) -> {
      attributes.addAttribute("", key, key, "CDATA", value.toString());
    });
    return attributes;
  }
}
