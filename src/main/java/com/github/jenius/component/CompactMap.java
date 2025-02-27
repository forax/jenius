package com.github.jenius.component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

class CompactMap<K, V> extends AbstractMap<K, V> {
  private final LinkedHashMap<K, V> map;

  private CompactMap(LinkedHashMap<K, V> map) {
    this.map = map;
  }

  public static <K, V> CompactMap<K, V> of() {
    return new CompactMap<>(new LinkedHashMap<>());
  }

  public static <K, V> CompactMap<K, V> of(K key1, V value1) {
    Objects.requireNonNull(key1);
    Objects.requireNonNull(value1);
    var map = new LinkedHashMap<K, V>();
    map.put(key1, value1);
    return new CompactMap<>(map);
  }

  public static <K, V> CompactMap<K, V> of(K key1, V value1, K key2, V value2) {
    Objects.requireNonNull(key1);
    Objects.requireNonNull(value1);
    Objects.requireNonNull(key2);
    Objects.requireNonNull(value2);
    var map = new LinkedHashMap<K, V>();
    map.put(key1, value1);
    map.put(key2, value2);
    return new CompactMap<>(map);
  }

  public static <K, V> CompactMap<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3) {
    Objects.requireNonNull(key1);
    Objects.requireNonNull(value1);
    Objects.requireNonNull(key2);
    Objects.requireNonNull(value2);
    Objects.requireNonNull(key3);
    Objects.requireNonNull(value3);
    var map = new LinkedHashMap<K, V>();
    map.put(key1, value1);
    map.put(key2, value2);
    map.put(key3, value3);
    return new CompactMap<>(map);
  }

  public static <K, V> CompactMap<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
    Objects.requireNonNull(key1);
    Objects.requireNonNull(value1);
    Objects.requireNonNull(key2);
    Objects.requireNonNull(value2);
    Objects.requireNonNull(key3);
    Objects.requireNonNull(value3);
    Objects.requireNonNull(key4);
    Objects.requireNonNull(value4);
    var map = new LinkedHashMap<K, V>();
    map.put(key1, value1);
    map.put(key2, value2);
    map.put(key3, value3);
    map.put(key4, value4);
    return new CompactMap<>(map);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return Collections.unmodifiableSet(map.entrySet());
  }

  @Override
  public Set<K> keySet() {
    return Collections.unmodifiableSet(map.keySet());
  }

  @Override
  public Collection<V> values() {
    return Collections.unmodifiableCollection(map.values());
  }

  @Override
  public boolean containsKey(Object key) {
    Objects.requireNonNull(key);
    return map.containsKey(key);
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    Objects.requireNonNull(key);
    return map.getOrDefault(key, defaultValue);
  }

  @Override
  public V get(Object key) {
    return getOrDefault(key, null);
  }
}
