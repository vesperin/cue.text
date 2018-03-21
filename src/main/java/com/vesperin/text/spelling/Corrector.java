package com.vesperin.text.spelling;

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
}
