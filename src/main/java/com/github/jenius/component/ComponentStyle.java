package com.github.jenius.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface ComponentStyle {
  Optional<Component> lookup(String name);

  default ComponentStyle ignoreAllOthers() {
    return anyMatch(this, _ -> Optional.of(Component.discard()));
  }

  static ComponentStyle of(String name, Component component) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(component);
    return of(Map.of(name, component));
  }

  static ComponentStyle of(Object... nameOrComponents) {
    Objects.requireNonNull(nameOrComponents);
    var map = new HashMap<String, Component>();
    var i = 0;
    Object nameOrComponent;
    for(;;) {
      var names = new ArrayList<String>();
      for(;;) {
        if (i == nameOrComponents.length) {
          if (!names.isEmpty()) {
            throw new IllegalArgumentException("names " + names + " with no componenet");
          }
          return of(map);
        }
        nameOrComponent = Objects.requireNonNull(nameOrComponents[i]);
        if (nameOrComponent instanceof String name) {
          names.add(name);
          i++;
          continue;
        }
        break;
      }
      if (!(nameOrComponent instanceof Component component)) {
        throw new IllegalArgumentException("invalid argument " + nameOrComponent);
      }
      for(var name : names) {
        map.put(name,  component);
      }
      i++;
    }
  }

  static ComponentStyle of(Map<? extends String, ? extends Component> componentMap) {
    Objects.requireNonNull(componentMap);
    return name -> Optional.ofNullable(componentMap.get(name));
  }

  static ComponentStyle rename(String... pairs) {
    Objects.requireNonNull(pairs);
    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException("not an array of pairs");
    }
    var map = new HashMap<String, String>();
    for(var i = 0; i < pairs.length; i += 2) {
      var oldName = Objects.requireNonNull(pairs[i]);
      var newName = Objects.requireNonNull(pairs[i + 1]);
      map.put(oldName, newName);
    }
    return name -> Optional.ofNullable(map.get(name))
          .map(newName -> (_, attrs, b) -> b.node(newName, attrs));
  }

  static ComponentStyle anyMatch(ComponentStyle... styles) {
    Objects.requireNonNull(styles);
    return name -> {
      for(var style : styles) {
        var componentOpt = style.lookup(name);
        if (componentOpt.isPresent()) {
          return componentOpt;
        }
      }
      return Optional.empty();
    };
  }

  static ComponentStyle alwaysMatch(Component component) {
    Objects.requireNonNull(component);
    return _ -> Optional.of(component);
  }
}
