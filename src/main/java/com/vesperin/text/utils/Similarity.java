package com.vesperin.text.utils;

import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class Similarity {
  private Similarity(){
    throw new Error("Cannot be instantiated");
  }

  /**
   * Calculates the editDistanceScore between two strings.
   * @param word original string
   * @param suggestion suggested string
   * @return editDistanceScore score.
   */
  public static float editDistanceScore(String word, String suggestion){
    return 1.0f - normalizedEditDistance(word, suggestion);
  }

  /**
   * Calculates the normalized distance of a suggested correction. This is
   * no longer a metric. Therefore, in order to calculate the editDistanceScore
   * between two words we must subtract this value from 1 (see
   * {@link #editDistanceScore(String, String)} method for details).
   *
   *
   * @param word original word
   * @param suggestion suggested correction for original word
   * @return minimum score to use.
   */
  private static float normalizedEditDistance(String word, String suggestion){
    Objects.requireNonNull(word);
    Objects.requireNonNull(suggestion);


    final float editDistance = (editDistance(word, suggestion)/1.0f);
    final float length       = Math.max(word.length(),suggestion.length())/1.0f;

    return (editDistance/length);
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
  private static int lcSubstr(String x, String y){
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
}
