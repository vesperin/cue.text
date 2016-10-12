package com.vesperin.text.spelling;

import java.util.Collection;

/**
 * @author Huascar Sanchez
 */
public interface BagOfWords {
  /**
   * Adds a new word to the stop-words list.
   *
   * @param word new word to add
   */
  void add(String word);

  /**
   * Adds a list of words to the stop-words object.
   *
   * @param words list of words to add
   */
  default void addAll(Collection<String> words){
    words.forEach(this::add);
  }
}
