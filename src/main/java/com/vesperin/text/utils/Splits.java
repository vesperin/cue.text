package com.vesperin.text.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;


/**
 * @author Huascar Sanchez
 */
public final class Splits {

  private static final String CAMEL_CASE = "((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|((?<=[a-zA-Z])(?=[0-9]))|((?<=[0-9])(?=[a-zA-Z]))|_";
  private static final String SEPARATORS = " \r\n\t.,;:'\"()?!\\-/|" + CAMEL_CASE;

  private static final Pattern SEPARATORS_PATTERN = Pattern
    .compile("[ \r\n\t.,;:'\"()?!\\-/]|" + CAMEL_CASE);

  private static final char[] CHARACTER_REPLACE_MAPPING = new char[256];
  static {
    int lowerDifference = 'a' - 'A';

    for (char i = 'A'; i <= 'Z'; i++) {
      CHARACTER_REPLACE_MAPPING[i] = ((char) (i + lowerDifference));
    }

    for (char i = '0'; i <= '9'; i++) {
      CHARACTER_REPLACE_MAPPING[i] = i;
    }

    for (char i = 'a'; i <= 'z'; i++) {
      CHARACTER_REPLACE_MAPPING[i] = i;
    }
  }

  private Splits() {
    throw new Error();
  }

  /**
   * Applies given regex on tokens and may optionally delete when a token gets
   * empty.
   */
  public static String[] removeMatchingRegex(String regex, String replacement,
                                             String[] tokens, boolean removeEmpty) {
    String[] tk = new String[tokens.length];
    for (int i = 0; i < tokens.length; i++) {
      tk[i] = tokens[i].replaceAll(regex, replacement);
    }

    if (removeEmpty) {
      tk = removeEmpty(tk);
    }
    return tk;
  }


  /**
   * Deduplicates the given tokens, but maintains the order.
   */
  public static String[] deduplicateTokens(String[] tokens) {
    LinkedHashSet<String> set = new LinkedHashSet<>();
    Collections.addAll(set, tokens);
    return set.toArray(new String[set.size()]);
  }

  /**
   * Tokenize on several indicators of a word, regex is [
   * \r\n\t.,;:'\"()?!\\-/|]
   */
  public static String[] wordTokenize(String text) {
    return wordTokenize(text, false);
  }

  /**
   * Tokenize like {@link #wordTokenize(String)} does, but keeps the separators
   * as their own token if the argument is true.
   */
  public static String[] wordTokenize(String text, boolean keepSeparators) {
    if (keepSeparators) {
      StringTokenizer tokens = new StringTokenizer(text, SEPARATORS, true);
      int countTokens = tokens.countTokens();
      String[] toReturn = new String[countTokens];
      int i = 0;
      while (countTokens-- > 0) {
        toReturn[i] = tokens.nextToken();
        if (toReturn[i].charAt(0) > ' ') {
          i++;
        }
      }
      return Arrays.copyOf(toReturn, i);
    } else {
      return SEPARATORS_PATTERN.split(text);
    }
  }

  /**
   * Tokenize on several indicators of a word, regex to detect these must be
   * given.
   */
  public static String[] wordTokenize(String text, String regex) {
    return text.split(regex);
  }

  /**
   * Normalizes the tokens:<br/>
   * - lower cases <br/>
   * - removes not alphanumeric characters.
   */
  public static String[] normalizeTokens(String[] tokens, boolean removeEmpty) {
    for (int i = 0; i < tokens.length; i++) {
      tokens[i] = normalizeString(tokens[i]);
    }

    if (removeEmpty) {
      tokens = removeEmpty(tokens);
    }

    return tokens;
  }

  /**
   * Normalizes the token:<br/>
   * - lower cases <br/>
   * - removes non alphanumeric characters.
   */
  public static String normalizeString(String token) {
    char[] charArray = token.toCharArray();
    char[] toReturn = new char[charArray.length];
    int index = 0;

    for (char x : charArray) {
      if (x < CHARACTER_REPLACE_MAPPING.length) {
        if (CHARACTER_REPLACE_MAPPING[x] > 0) {
          toReturn[index++] = CHARACTER_REPLACE_MAPPING[x];
        }
      }
    }

    return String.valueOf(Arrays.copyOf(toReturn, index));
  }

  /**
   * Removes empty tokens from given array. The empty slots will be filled with
   * the follow-up tokens.
   */
  public static String[] removeEmpty(String[] arr) {
    ArrayList<String> list = new ArrayList<>();
    for (String s : arr) {
      if (s != null && !s.isEmpty())
        list.add(s);
    }
    return list.toArray(new String[list.size()]);
  }

  /**
   * Removes illegal tokens from given array. The illegal slots will be filled with
   * the follow-up tokens.
   */
  public static String[] removeIllegal(String[] arr){
    return removeIllegal(arr, true);
  }

  /**
   * Removes illegal tokens from given array. The illegal slots will be filled with
   * the follow-up tokens.
   */
  public static String[] removeIllegal(String[] arr, boolean camelizeFirst) {
    final String[] nonEmpty = removeEmpty(arr);

    final List<String> list = new ArrayList<>();
    for (String s : nonEmpty) {

      if (s != null
        && (s.length() > 1
        && !Strings.isNumber(s)
        && !Strings.onlyConsonantsOrVowels(s))) {

        s = camelizeFirst ? Strings.firstCharUpperCase(s) : s;
        list.add(s);
      }
    }
    return list.toArray(new String[list.size()]);
  }


  public static void main(String[] args) {

    final String[] words = removeIllegal(wordTokenize("LaV1da_DaTriangle"));
    System.out.println(Arrays.toString(words));
  }

}