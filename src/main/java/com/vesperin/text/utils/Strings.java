package com.vesperin.text.utils;

import com.google.common.collect.Sets;
import com.vesperin.text.spelling.Corrector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
public class Strings {
  private Strings(){}

  public static double similarity(String word, String suggestion){
    return Similarity.damerauLevenshteinScore(word, suggestion);
  }

  public static String[] wordSplit(String word){
    return Splits.removeIllegal(Splits.wordTokenize(word));
  }

  public static Set<String> intersect(String x, String y){
    return Strings.intersect(
      Strings.wordSplit(x),
      Strings.wordSplit(y)
    );
  }

  public static Set<String> intersect(String[] a, String[] b){
    return intersect(Arrays.asList(a), Arrays.asList(b));
  }

  public static String sharedSuffix(String x, String y){
    int m = x.length() - 1;
    int n = y.length() - 1;

    int k = 0;
    String suffix;

    while(m >= 0 && n >= 0 && x.substring(m).equals(y.substring(n))){
      m--;
      n--;
      k++;
    }

    suffix = x.substring((x.length() - (k)), x.length());
    return suffix;
  }

  private static Set<String> intersect(List<String> a, List<String> b){
    final Predicate<String> skipNumbers     = w -> !Corrector.isNumber(w);
    final Predicate<String> skipSingleChar  = w -> w.length() > 1 && !w.isEmpty();
    final Set<String> aa = a.stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());
    final Set<String> bb = b.stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());

    return Sets.intersection(aa, bb);
  }

  /**
   * Choose the K value from a list of strings.
   * @param data list of strings.
   * @return the k value
   */
  public static int chooseK(List<String> data){
    if(Objects.isNull(data)) return 0;
    if(data.isEmpty())       return 0;

    return (int) Math.ceil(Math.sqrt(data.size()));
  }

  /**
   * Sorts string by their typicality (or membership); i.e., how closely related each string
   * element is to the rest of string elements in that list.
   *
   * @param data unsorted list of string elements.
   * @return a new sorted list of string elements.
   */
  public static List<String> typicalityRank(List<String> data){
    return typicalityRank(chooseK(data), data);
  }


  /**
   * Sorts string by their typicality (or membership); i.e., how closely related each string
   * element is to the rest of string elements in that list.
   *
   * @param k number of string elements (in the list) to return.
   * @param data unsorted list of string elements.
   * @return a new sorted list of string elements.
   */
  public static List<String> typicalityRank(int k, List<String> data){

    if(data.size() < 2) return data;

    k = k < 1 ? chooseK(data) : k;

    Collections.shuffle(data);

    final Map<String, Double> T = typicalityQuery(data);

    final List<String> ranked = T.keySet().stream()
      .sorted((a, b) -> Double.compare(T.get(b), T.get(a)))
      .collect(toList());

    return ranked.stream()
      .filter(s -> !Objects.isNull(s)).limit(k)
      .collect(toList());
  }

  /**
   * Generates the typicality region for a list of strings.
   *
   * @param club the list of strings.
   * @return the typicality region
   */
  public static Map<String, Double> typicalityQuery(List<String> club){
    final Map<StringKey, Double> T = new HashMap<>();

    final Set<StringKey>       clubSet   = club.stream().map(StringKey::new).collect(Collectors.toSet());
    final Set<List<StringKey>> cartesian = Sets.cartesianProduct(Arrays.asList(clubSet, clubSet));

    for(StringKey code : clubSet){ T.put(code, 0.0D); }

    double t1  = 1.0D / (T.keySet().size()) * Math.sqrt(2.0 * Math.PI);
    double t2  = 2.0 * Math.pow(0.3, 2);

    for(List<StringKey> pair : cartesian){
      final StringKey si = pair.get(0);
      final StringKey sj = pair.get(1);

      double w = gaussianKernel(t1, t2, si.value, sj.value);

      double Tsi = T.get(si) + w;
      double Tsj = T.get(sj) + w;

      T.put(si, Tsi);
      T.put(sj, Tsj);
    }

    final Map<String, Double>   R = new HashMap<>();

    for(StringKey each : T.keySet()){
      if(!R.containsKey(each.value)){
        R.put(each.value, T.get(each));
      } else {
        final double update   = T.get(each);
        final double current  = R.get(each.value);
        final double max      = Math.max(update, current);

        R.put(each.value, max);
      }
    }

    return R;
  }

  private static class StringKey {
    String value;

    StringKey(String value) {
      this.value = value;
    }
  }

  private static double round(double value, int places){
    return new BigDecimal(value)
      .setScale(places, RoundingMode.HALF_UP)
      .doubleValue();
  }

  private static double gaussianKernel(double t1, double t2, String oi, String oj){
    final double distance = Similarity.jaccardDistance(oi, oj);
    return t1 * Math.exp(-(Math.pow(distance, 2) / t2));
  }

  public static int lcSuffix(String x, String y){
    return sharedSuffix(x, y).length();
  }

  public static String cleanup(String text){

    String curated = text;
    boolean endsWithTripleLetters = curated.matches(".*[A-Z]{3}");
    if(endsWithTripleLetters){
      curated = curated.replaceAll("[A-Z]{3}", "");
    } else {
      boolean endsWithPattern = curated.matches(".*[0-9]+[a-zA-Z]([0-9])*")
        && Character.isDigit(curated.charAt(curated.length() - 1));

      if (endsWithPattern){ // 2x2 or alike
        curated = curated.replaceAll("[0-9]+[a-zA-Z]([0-9])*", "");
      } else {

        endsWithPattern = curated.matches(".*[0-9]+[a-z]*")
          && Character.isLowerCase(curated.charAt(curated.length() - 1));

        if (endsWithPattern){
          curated = curated.replaceAll("[0-9]+[a-z]+", "");
        } else {

          endsWithPattern = containsSubstringPattern(curated);
          if(endsWithPattern){
            curated = replaceAll(curated);
          } else {
            curated = curated.replaceAll("([0-9])*", "");
          }

        }

      }
    }

    curated = curated.isEmpty() ? text : curated;
    curated = Character.isUpperCase(curated.charAt(curated.length() - 1))
      ? curated.substring(0, curated.length() - 1)
      : curated;

    return curated;
  }

  private static boolean containsSubstringPattern (String word){
    return (word.contains("2D") && !word.endsWith("2D"))
      || (word.contains("3D") && !word.endsWith("3D"));
  }

  private static String replaceAll(String word){
    final Set<String> matches = new HashSet<>();
    matches.add("2D");
    matches.add("3D");

    return replaceMatching(word, matches);
  }

  private static String replaceMatching(String word, Set<String> matches){
    for(String match : matches){
      if(word.contains(match)) {
        word = word.replace(match, "");
      }
    }

    return word;
  }

  public static String firstCharUpperCase(String word){
    Objects.requireNonNull(word);

    if(word.isEmpty())       return word;
    if(Character.isUpperCase(word.charAt(0))) return word;

    return Character.toUpperCase(word.charAt(0)) + word.substring(1);
  }

  public static void main(String[] args) {
    String[] x = Strings.wordSplit("La vida \n super sonicCamel");
    Arrays.stream(x).forEach(System.out::println);
  }

}
