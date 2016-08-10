package com.vesperin.text.utils;

import com.google.common.collect.Sets;
import com.vesperin.text.spelling.Corrector;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Strings {
  private Strings(){}

  public static String[] splits(String word){
    String[] split = word.split("((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|((?<=[a-zA-Z])(?=[0-9]))|((?<=[0-9])(?=[a-zA-Z]))|_");
    if(split.length == 1){
      split = split[0].split(Pattern.quote("_"));
    }

    return split;
  }

  public static Set<String> intersect(String[] a, String[] b){
    final Predicate<String> skipNumbers     = w -> !Corrector.isNumber(w);
    final Predicate<String> skipSingleChar  = w -> w.length() > 1 && !w.isEmpty();
    final Set<String> aa = Arrays.asList(a).stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());
    final Set<String> bb = Arrays.asList(b).stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());

    return Sets.intersection(aa, bb);
  }
}
