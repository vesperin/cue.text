package com.vesperin.text.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.spi.ExecutionMonitor;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
public class Strings {
  private static ExecutionMonitor LOGGER = BasicExecutionMonitor.get();
  private Strings(){}

  public static String[] wordSplit(String word){
    return Splits.removeIllegal(Splits.tokenizeWordWithDictCheck(word));
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

  public static Set<String> intersect(List<String> a, List<String> b){
    final Predicate<String> skipNumbers     = w -> !isNumber(w);
    final Predicate<String> skipSingleChar  = w -> w.length() > 1;
    final Set<String> aa = a.stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());
    final Set<String> bb = b.stream().filter(skipNumbers.or(skipSingleChar)).collect(Collectors.toSet());

    return Sets.intersection(aa, bb);
  }

  /**
   * Sorts string by their typicality (or membership); i.e., how closely related each string
   * element is to the rest of string elements in that list.
   *
   * @param data unsorted list of string elements.
   * @return a new sorted list of string elements.
   */
  public static List<String> typicalityRank(List<String> data){
    if(Objects.isNull(data)) return Collections.emptyList();
    return typicalityRank(data.size(), data);
  }

  /**
   * Re-ranks most typical words by representativeness. A typical word is representative
   * of its group if this word covers the max number of words in PopulationSet - TypicalSet.
   *
   * @param typicalSet set of typical words
   * @param universe set of frequent words
   * @return a list of words ordered by representativeness.
   */
  public static List<String> representativenessRank(Set<String> typicalSet, Set<String> universe){

    final Set<String> difference = Sets.difference(universe, typicalSet);

    final Map<String, List<String>> coverageRegion = generateCoverageRegion(typicalSet, difference);

    final List<String> result = coverageRegion.entrySet().stream()
      .filter(e -> !e.getValue().isEmpty())
      .sorted((a, b) -> Integer.compare((b.getValue().size()), (a.getValue().size())))
      .map(Map.Entry::getKey)
      .collect(toList());

    logRepresentativenessResults("Representativeness Rank:", coverageRegion, result);

    return result;
  }

  private static void logRepresentativenessResults(String entering, Map<String, List<String>> coverageRegion, List<String> result){
    if(!LOGGER.isActive()) return;

    if(coverageRegion.isEmpty()){
      LOGGER.info(entering + " " + "Empty coverage region. No representative words!");
    } else {

      final double sum = result.stream().mapToDouble(s -> coverageRegion.get(s).size()).sum();

      final Map<String, Double> temp = new HashMap<>();

      result.forEach(s -> temp.put(s, coverageRegion.get(s).size()/sum));

      LOGGER.info(entering + " " + Prints.toPrettyPrintedMap(Prints.sortByValue(temp)));
    }

  }

  private static void logTypicalityResults(String entering, Map<String, Double> typicalityRanks, List<String> limitedResult){
    if(!LOGGER.isActive()) return;

    final Map<String, Double> sorted = new HashMap<>();
    limitedResult.forEach(s -> sorted.put(s, typicalityRanks.get(s)));

    LOGGER.info(entering + " " + Prints.toPrettyPrintedMap(Prints.sortByValue(sorted)));
  }

  /**
   * Generates a region of representativeness for a set of typical words.
   *
   * @param typicalSet the set of typical words
   * @param difference the set of words in the set of all words not in the typical set.
   * @return a mapping between typical word and list of words covered by this word.
   */
  public static Map<String, List<String>> generateCoverageRegion(Set<String> typicalSet, Set<String> difference) {
    final Map<String, List<String>> coverageRegion = new HashMap<>();

    // init coverage map
    for(String each : typicalSet){
      coverageRegion.put(each, new ArrayList<>());
    }


    for(String e : difference){
      String min = Iterables.get(typicalSet, 0);
      for(String t : typicalSet){

        final double eoDistance = Similarity.jaccardDistance(e, t);
        final double moDistance = Similarity.jaccardDistance(e, min);

        if(Double.compare(eoDistance, moDistance) < 0){
          min = t;
        }
      }

      coverageRegion.get(min).add(e);

    }

    return coverageRegion;
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

    k = k < 1 ? Samples.chooseK(data) : k;

    Collections.shuffle(data);

    final Map<String, Double> T = typicalityQuery(data);

    final List<String> ranked = T.keySet().stream()
      .sorted((a, b) -> Double.compare(T.get(b), T.get(a)))
      .collect(toList());

    final List<String> result = ranked.stream()
      .filter(s -> !Objects.isNull(s)).limit(k)
      .collect(toList());


    logTypicalityResults("Typicality Rank:", T, result);

    return result;
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

  public static boolean isNumber(String input) {
    // thx to http://stackoverflow.com/q/15111420/15111450
    return !(input == null || input.isEmpty()) && input.matches("\\d+");
  }

  public static boolean startsWithNumbers(String input){
    return !(input == null || input.isEmpty()) && Character.isDigit(input.charAt(0));
  }

  public static boolean endsWithNumbers(String input){
    return !(input == null || input.isEmpty()) && Character.isDigit(input.charAt(input.length() - 1));
  }

  public static String trimLeft(String input){
    final char[] chars = input.toCharArray();
    int to = 0; for(char each : chars){
      if(Character.isAlphabetic(each)) { break; }
      if(Character.isDigit(each))      { to++;  }
    }

    return input.substring(to, input.length());
  }

  public static String trimRight(String input){
    final char[] chars = input.toCharArray();
    int to = chars.length; for(int j = chars.length - 1; j >= 0; j--){
      final char each = chars[j];
      if(Character.isAlphabetic(each)) { break; }
      if(Character.isDigit(each))      { to--;  }
    }

    return input.substring(0, to);
  }

  public static boolean onlyConsonantsOrVowels(String word){
    return onlyConsonants(word) || onlyVowels(word);
  }

  public static boolean onlyConsonants(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty())
      && !hasAVowel(word.toLowerCase(Locale.ENGLISH));//.matches("^(?!.*(NG|ng)).[^aeyiuo]*$");
  }

  public static boolean onlyVowels(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty())
      && onlyConsonants(word);
  }

  public static boolean hasAVowel(final String input){
    for (int i = 0; i < input.length(); i++) {
      switch (input.charAt(i)) {
        case 'a':
          return true;
        case 'e':
          return true;
        case 'i':
          return true;
        case 'o':
          return true;
        case 'u':
          return true;
      }
    }

    return false;
  }

  public static String trimSideNumbers(String each, boolean lowercase){
    String updatedEach;
    if(startsWithNumbers(each) || endsWithNumbers(each)) {
      updatedEach = trimRight(trimLeft(each));
      updatedEach = lowercase ? updatedEach.toLowerCase(Locale.ENGLISH) : updatedEach;
    } else {
      updatedEach = lowercase ? each.toLowerCase(Locale.ENGLISH) : each;
    }

    return updatedEach;
  }

  private static class StringKey {
    String value;

    StringKey(String value) {
      this.value = value;
    }
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

    final int idx = curated.lastIndexOf("$");
    curated = idx > 0 ? curated.substring(0, idx) : curated;

    curated = curated.isEmpty() ? text : curated;

    if(curated.isEmpty()){
      return curated;
    }

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
