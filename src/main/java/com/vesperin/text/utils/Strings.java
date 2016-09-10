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
  private static final String CAMEL_CASE = "((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|((?<=[a-zA-Z])(?=[0-9]))|((?<=[0-9])(?=[a-zA-Z]))|_";
  private Strings(){}

  public static String[] splits(String word){
    String[] split = word.split(CAMEL_CASE);
    if(split.length == 1){
      split = split[0].split(Pattern.quote("_"));
//      if(split.length == 1){
//
//        final boolean firstConsonant  = isConsonant(split[0].charAt(0));
//        final boolean secondConsonant = isConsonant(split[0].charAt(1));
//        if(firstConsonant && secondConsonant){
//          split = new String[]{
//            String.valueOf(Character.toUpperCase(split[0].charAt(0))),
//            split[0].substring(1)
////            Character.toUpperCase(split[0].charAt(1)) + split[0].substring(2)
//          };
//        }
//      }
    }

    return split;
  }

  private static boolean isConsonant(final char ch){
    return !isVowel(ch);
  }

  private static boolean isVowel(final char ch) {
    return ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u';
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

    final Map<String, Double> T = typicalityRegion(club);

    final List<String> ranked = T.keySet().stream()
      .sorted((a, b) -> Double.compare(T.get(a), T.get(b)))
      .collect(toList());

    return ranked.stream()
      .filter(s -> !Objects.isNull(s) && !stopWords.contains(s)).limit(k)
      .collect(toList());
  }

  /**
   * Generates the typicality region for a list of strings.
   *
   * @param club the list of strings.
   * @return the typicality region
   */
  public static Map<String, Double> typicalityRegion(List<String> club){
    final Map<String, Double> T = new HashMap<>();

    final Set<String>       clubSet   = club.stream().collect(Collectors.toSet());
    final Set<List<String>> cartesian = Sets.cartesianProduct(Arrays.asList(clubSet, clubSet));

    for(String code : clubSet){
      T.put(code, 0.0D);
    }

    double t1  = 1.0D / (T.keySet().size()) * Math.sqrt(2.0 * Math.PI);
    double t2  = 2.0 * Math.pow(0.3, 2);

    for(List<String> pair : cartesian){
      final String si = pair.get(0);
      final String sj = pair.get(1);

      double w = gaussianKernel(t1, t2, si, sj);

      double Tsi = T.get(si) + w;
      double Tsj = T.get(sj) + w;

      T.put(si, Tsi);
      T.put(sj, Tsj);
    }

   return T;
  }

  private static double gaussianKernel(double t1, double t2, String oi, String oj){
    return t1 * Math.exp(-(Math.pow(Similarity.damerauLevenshteinScore(oi, oj), 2) / t2));
  }
}
