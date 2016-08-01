package com.vesperin.text.spelling;

import java.util.Locale;

/**
 * @author Huascar Sanchez
 */
interface Corrector {

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

  static boolean onlyConsonants(String word) {
    // thx to http://stackoverflow.com/q/26536829/26536928
    return !(word == null || word.isEmpty())
      && word.toLowerCase(Locale.ENGLISH).matches("^[aeiuoAEIOU]*$|^(?!.*(NG|ng)).[^aeyiuo]*$");
  }

  static boolean isNumber(String input) {
    // thx to http://stackoverflow.com/q/15111420/15111450
    return !(input == null || input.isEmpty()) && input.matches("\\d+");
  }

  static boolean isAlphanumeric(String input){
    return !(input == null || input.isEmpty()) && input.toLowerCase(Locale.ENGLISH).matches("^(?=.*[a-z])(?=.*[0-9])[a-z0-9]+$");
  }
}
