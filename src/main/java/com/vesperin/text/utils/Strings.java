package com.vesperin.text.utils;

import com.google.common.collect.Sets;
import com.vesperin.text.spelling.Corrector;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
    return intersect(Arrays.asList(a), Arrays.asList(b));
  }

  private static Set<String> intersect(List<String> a, List<String> b){
    final Predicate<String> skipNumbers     = w -> !Corrector.isNumber(w);
    final Predicate<String> skipSingleChar  = w -> w.length() > 1 && !w.isEmpty();
    final Set<String> aa = a.stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());
    final Set<String> bb = b.stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());

    return Sets.intersection(aa, bb);
  }


  /**
   * Sorts string by their typicality (or membership); i.e., how closely related each string
   * element is to the rest of string elements in that list.
   *
   * @param k number of string elements (in the list) to return.
   * @param club unsorted list of string elements.
   * @param stopWords words we should ignore.
   * @return a new sorted list of string elements.
   */
  public static List<String> typicalitySorting(int k, List<String> club, final Set<String> stopWords){

    if(club.size() < 2) return club;

    k = k < 1 ? (int) Math.ceil(Math.sqrt(club.size())) : k;

    Collections.shuffle(club);

    final Set<String>       clubSet   = club.stream().collect(Collectors.toSet());
    final Set<List<String>> cartesian = Sets.cartesianProduct(Arrays.asList(clubSet, clubSet));

    final Map<String, Double> T = new HashMap<>();

    for(String code : clubSet){
      T.put(code, 0.0D);
    }

    for(List<String> pair : cartesian){
      final String si = pair.get(0);
      final String sj = pair.get(1);

      double w = Similarity.lcSubstrScore(si, sj);

      double Tsi = T.get(si) + w;
      double Tsj = T.get(sj) + w;

      T.put(si, Tsi);
      T.put(sj, Tsj);
    }

    final List<String> ranked = T.keySet().stream()
      .sorted((a, b) -> Double.compare(T.get(b), T.get(a)))
      .collect(toList());

    return ranked.stream()
      .filter(s -> !Objects.isNull(s) && !stopWords.contains(s)).limit(k)
      .collect(toList());
  }
}
