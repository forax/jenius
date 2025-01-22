package com.github.jenius.component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface ComponentStyle {
  Optional<Component> lookup(String name);

  default ComponentStyle ignoreAllOthers() {
    return anyMatch(this, _ -> Optional.of(Component.ignore()));
  }

  static ComponentStyle of(String name, Component component) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(component);
    return of(Map.of(name, component));
  }

  static ComponentStyle of(Map<? extends String, ? extends Component> componentMap) {
    Objects.requireNonNull(componentMap);
    return name -> Optional.ofNullable(componentMap.get(name));
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
