package com.vesperin.text.spelling;

import java.util.Locale;

/**
 * @author Huascar Sanchez
 */
public interface Corrector {

  /**
   * Provides the best word correction for a given word. This word will have a
   * word accuracy of at least 0.5f, per default values.
   *
   * @param word word to be corrected.
   * @return a list of suggested word corrections.
   */
  default String correct(String word){
    return correct(word, 0.5f);
  }

  /**
   * Provides a list of corrections for a word.
   *
   * @param word word to be corrected.
   * @param accuracy minimum score to use.
   * @return a list of suggested word corrections.
   */
  String correct(String word, float accuracy);

  static boolean onlyConsonantsOrVowels(String word){
    return onlyConsonants(word) || onlyVowels(word);
  }

  static boolean onlyConsonants(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty())
      && !hasAVowel(word.toLowerCase(Locale.ENGLISH));//.matches("^(?!.*(NG|ng)).[^aeyiuo]*$");
  }

  static boolean onlyVowels(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty())
      && onlyConsonants(word);
  }

  static boolean hasAVowel(final String input){
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

  static boolean isNumber(String input) {
    // thx to http://stackoverflow.com/q/15111420/15111450
    return !(input == null || input.isEmpty()) && input.matches("\\d+");
  }

  static boolean startsWithNumbers(String input){
    return !(input == null || input.isEmpty()) && Character.isDigit(input.charAt(0));
  }

  static boolean endsWithNumbers(String input){
    return !(input == null || input.isEmpty()) && Character.isDigit(input.charAt(input.length() - 1));
  }

  static String trimLeft(String input){
    final char[] chars = input.toCharArray();
    int to = 0; for(char each : chars){
      if(Character.isAlphabetic(each)) { break; }
      if(Character.isDigit(each))      { to++;  }
    }

    return input.substring(to, input.length());
  }

  static String trimRight(String input){
    final char[] chars = input.toCharArray();
    int to = chars.length; for(int j = chars.length - 1; j >= 0; j--){
      final char each = chars[j];
      if(Character.isAlphabetic(each)) { break; }
      if(Character.isDigit(each))      { to--;  }
    }

    return input.substring(0, to);
  }
}
