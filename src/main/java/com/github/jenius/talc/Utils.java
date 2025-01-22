package com.github.jenius.talc;

public class Utils {
  private Utils() {
    throw new AssertionError();
  }

  public static String removeExtension(String filename) {
    var index = filename.lastIndexOf('.');
    return index == -1 ? filename : filename.substring(0, index);
  }
}
