package com.vesperin.text.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
public class Similarity {
  private static final Pattern SPACE_REG = Pattern.compile("\\s+");

  private Similarity(){
    throw new Error("Cannot be instantiated");
  }

  /**
   * Normalizes a distance value between two strings.
   *
   * @param distance distance between two strings
   * @param s1 first string
   * @param s2 second string
   * @return normalized distance.
   */
  public static double normalize(int distance, String s1, String s2){
    Objects.requireNonNull(s1);
    Objects.requireNonNull(s2);

    final int    length       = Math.max(s1.length(), s2.length());

    if(length == 0) return 0.0D;

    return (distance/(length * 1.0D));
  }


  /**
   * Computes the normalized Damerau–Levenshtein edit distance
   * (with adjacent transpositions) between two given strings.
   *
   * @param word first string
   * @param suggestion second string
   * @return normalized Damerau–Levenshtein edit distance
   */
  public static double damerauLevenshteinScore(String word, String suggestion){

    final int editDistance = (damerauLevenshteinDistance(word, suggestion));

    return 1.0D - normalize(editDistance, word, suggestion);
  }

  /**
   * Computes the true Damerau–Levenshtein edit distance
   * (with adjacent transpositions) between two given strings.<br><br>
   *
   * Based on <a href="http://en.wikipedia.org/wiki/Damerau–Levenshtein_distance">C# code from Wikipedia</a>.
   *
   * @param str1  First string being compared
   * @param str2  Second string being compared
   * @return      Edit distance between strings
   */
  public static int damerauLevenshteinDistance(String str1, String str2) {
    // return fast if one or both strings is empty or null
    if ((str1 == null) || str1.isEmpty()) {
      if ((str2 == null) || str2.isEmpty()) {
        return 0;
      } else {
        return str2.length();
      }
    } else if ((str2 == null) || str2.isEmpty()) {
      return str1.length();
    }

    // split strings into string arrays
    String[] stringArray1 = str1.split("");
    String[] stringArray2 = str2.split("");

    // initialize matrix values
    int[][] matrix = new int[stringArray1.length + 2][stringArray2.length + 2];
    int bound = stringArray1.length + stringArray2.length;
    matrix[0][0] = bound;
    for (int i = 0; i <= stringArray1.length; i++) {
      matrix[i + 1][1] = i;
      matrix[i + 1][0] = bound;
    }
    for (int j = 0; j <= stringArray2.length; j++) {
      matrix[1][j + 1] = j;
      matrix[0][j + 1] = bound;
    }

    // initialize dictionary
    SortedMap<String, Integer> dictionary = new TreeMap<>();
    for (String letter : (str1 + str2).split("")) {
      if (!dictionary.containsKey(letter)) {
        dictionary.put(letter, 0);
      }
    }

    // compute edit distance between strings
    for (int i = 1; i <= stringArray1.length; i++) {
      int index = 0;
      for (int j = 1; j <= stringArray2.length; j++) {
        int i1 = dictionary.get(stringArray2[j - 1]);
        int j1 = index;
        if (stringArray1[i - 1].equals(stringArray2[j - 1])) {
          matrix[i + 1][j + 1] = matrix[i][j];
          index = j;
        } else {
          matrix[i + 1][j + 1] = Math.min(matrix[i][j], Math.min(matrix[i + 1][j], matrix[i][j + 1])) + 1;
        }

        matrix[i + 1][j + 1] = Math.min(matrix[i + 1][j + 1], matrix[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1));
      }

      dictionary.put(stringArray1[i - 1], i);
    }

    return matrix[stringArray1.length + 1][stringArray2.length + 1];
  }

  /**
   * Calculates the editDistanceScore between two strings.
   * @param word original string
   * @param suggestion suggested string
   * @return editDistanceScore score.
   */
  public static double editDistanceScore(String word, String suggestion){
    final int distance = editDistance(word, suggestion);
    return 1.0D - normalize(distance, word, suggestion);
  }

  /**
   * Edit distance between words
   *
   * @param a original word
   * @param b suggested correction.
   * @return the edit distance.
   */
  private static int editDistance(String a, String b){
    if(a == null || b == null)  return 0;
    if(a.length() == 0)         return 0;
    if(b.length() == 0)         return 0;
    if(a.equals(b))             return 0;


    int[] v0 = new int[b.length() + 1];
    int[] v1 = new int[b.length() + 1];

    int idx;
    for(idx = 0; idx < v0.length; idx++){
      v0[idx] = idx;
    }

    for(idx = 0; idx < a.length(); idx++){
      v1[0] = idx + 1;

      for (int j = 0; j < b.length(); j++){
        int cost = (a.charAt(idx) == b.charAt(j) ? 0 : 1);

        v1[j + 1] = Math.min(Math.min(v1[j] + 1, v0[j + 1] + 1), v0[j] + cost);
      }

      System.arraycopy(v1, 0, v0, 0, v0.length);
    }

    return v1[b.length()];
  }


  /**
   * Distance metric based on Longest Common Subsequence, from the notes "An
   * LCS-based string metric" by Daniel Bakkelund.
   *
   * @param s1 first string
   * @param s2 second string
   * @return lcs metric score
   */
  public static double lcsDistanceScore(String s1, String s2){
    return 1.0 - ((double) lcs(s1, s2)) / Math.max(s1.length(), s2.length());
  }

  /**
   * Longest common sub-sequence
   *
   * @param s1 first string
   * @param s2 second string
   * @return the longest common sub sequence
   */
  private static int lcs(String s1, String s2){

    int m = s1.length();
    int n = s2.length();
    char[] X = s1.toCharArray();
    char[] Y = s2.toCharArray();

    int[][] C = new int[m + 1][n + 1];

    for (int i = 0; i <= m; i++) {
      C[i][0] = 0;
    }

    for (int j = 0; j <= n; j++) {
      C[0][j] = 0;
    }

    for (int i = 1; i <= m; i++) {
      for (int j = 1; j <= n; j++) {
        if (X[i - 1] == Y[j - 1]) {
          C[i][j] = C[i - 1][j - 1] + 1;

        } else {
          C[i][j] = Math.max(C[i][j - 1], C[i - 1][j]);
        }
      }
    }

    return C[m][n];
  }

  /**
   * Distance metric based on Longest Common Substring, from Wikipedia:
   * {@code https://en.wikipedia.org/wiki/Longest_common_substring_problem}
   *
   * @param s1 first string
   * @param s2 second string
   * @return the longest common sub-string
   */
  public static double lcSubstrScore(String s1, String s2){
    return 1.0 - ((double) lcSubstr(s1, s2)) / Math.max(s1.length(), s2.length());
  }

  /* Returns length of longest common substring of X[0..m-1] and Y[0..n-1] */
  public static int lcSubstr(String x, String y){
    final char[] X = x.toCharArray();
    final char[] Y = y.toCharArray();

    int m = x.length();
    int n = y.length();

    // Create a table to store lengths of longest common suffixes of
    // sub-strings. Note that table[i][j] contains length of longest
    // common suffix of X[0..i-1] and Y[0..j-1]. The first row and
    // first column entries have no logical meaning, they are used only
    // for simplicity of program
    int[][] table = new int[m+1][n+1];
    int result = 0;  // To store length of the longest common substring

      /* Following steps build table[m+1][n+1] in bottom up fashion. */
    for (int i = 0; i <= m; i++){
      for (int j=0; j<=n; j++){
        if (i == 0 || j == 0) {
          table[i][j] = 0;
        } else if (X[i-1] == Y[j-1]){
          table[i][j] = table[i-1][j-1] + 1;
          result = Math.max(result, table[i][j]);
        } else {
          table[i][j] = 0;
        }
      }
    }

    return result;
  }


  public static double lcSuffixScore(String x, String y){
    return Similarity.normalize(Strings.lcSuffix(x, y), x, y);
  }

  /**
   * Compute and return the profile of s, as defined by Ukkonen "Approximate
   * string-matching with q-grams and maximal matches".
   * https://www.cs.helsinki.fi/u/ukkonen/TCS92.pdf
   * The profile is the number of occurrences of k-shingles, and is used to
   * compute q-gram similarity, Jaccard index, etc.
   * Pay attention: the memory requirement of the profile can be up to
   * k * size of the string
   *
   * @param string
   * @return the profile of this string, as an unmodifiable Map
   */
  private static Map<String, Integer> getProfile(final String string) {
    Map<String, Integer> shingles = new HashMap<>();

    String string_no_space = SPACE_REG.matcher(string).replaceAll(" ");
    for (int i = 0; i < (string_no_space.length() - 1/*default k*/ + 1); i++) {
      String shingle = string_no_space.substring(i, i + 1/*default k*/);

      if (shingles.containsKey(shingle)) {
        shingles.put(shingle, shingles.get(shingle) + 1);

      } else {
        shingles.put(shingle, 1);

      }
    }

    return Collections.unmodifiableMap(shingles);
  }

  /**
   * Compute Jaccard index: |A inter B| / |A union B|.
   * @param s1 first string
   * @param s2 second string
   * @return Jaccard index
   */
  public static double jaccard(final String s1, final String s2) {
    Map<String, Integer> profile1 = getProfile(s1);
    Map<String, Integer> profile2 = getProfile(s2);

    Set<String> union = new HashSet<>();
    union.addAll(profile1.keySet());
    union.addAll(profile2.keySet());

    int inter = 0;

    for (String key : union) {
      if (profile1.containsKey(key) && profile2.containsKey(key)) {
        inter++;
      }
    }

    double result = 1.0 * inter / union.size();
    result = Double.isNaN(result) ? 0.0 : result;

    return result;
  }

  /**
   * Computes the Jaccard distance between two strings s1 and s2.
   *
   * @param s1 first string
   * @param s2 second string
   * @return Jaccard distance
   */
  public static double jaccardDistance(String s1, String s2){
    return 1.0 - Similarity.jaccard(s1, s2);
  }

}
