package com.vesperin.text.spelling;

import java.util.Collection;

/**
 * @author Huascar Sanchez
 */
public interface Words <T> {
  /**
   * Adds a new thing to a list of things
   *
   * @param thing new thing to add
   */
  void add(T thing);

  /**
   * Adds a list of things to another list of things.
   *
   * @param words list of things to add
   */
  default void addAll(Collection<T> words){
    words.forEach(this::add);
  }
}
