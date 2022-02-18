package com.kodality.kefhir.search.util;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public final class SearchPathUtil {

  public static Set<String> parsePaths(String expression) {
    return Stream.of(expression.split("\\|"))
        .map(s -> StringUtils.trim(s))
        .map(s -> replaceAs(s))
        .map(s -> replaceFhirpathAs(s))
        .collect(Collectors.toSet());
  }

  private static String replaceAs(String s) {
    if (!s.startsWith("(") || !s.contains(" as ")) {
      return s;
    }
    s = StringUtils.remove(s, "(");
    s = StringUtils.remove(s, ")");
    String[] p = s.split(" as ");
    return p[0] + StringUtils.capitalize(p[1]);
  }

  private static String replaceFhirpathAs(String s) {
    if (!s.contains(".as(")) {
      return s;
    }
    String[] p = s.split(".as");
    p[1] = StringUtils.remove(p[1], ")");
    p[1] = StringUtils.remove(p[1], "(");
    return p[0] + StringUtils.capitalize(p[1]);
  }
}
