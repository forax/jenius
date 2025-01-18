package com.github.jenius.component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface ComponentStyle {
  Optional<Component> lookup(String name);

  static ComponentStyle of(String name, Component component) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(component);
    return of(Map.of(name, component));
  }

  static ComponentStyle of(Map<? extends String, ? extends Component> componentMap) {
    Objects.requireNonNull(componentMap);
    return name -> Optional.ofNullable(componentMap.get(name));
  }

  static ComponentStyle anyMatch(List<? extends ComponentStyle> styles) {
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
