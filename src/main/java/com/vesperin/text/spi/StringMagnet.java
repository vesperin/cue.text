package com.vesperin.text.spi;

import com.google.common.collect.Iterables;
import com.vesperin.text.groups.Magnet;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
public interface StringMagnet extends Magnet<Boolean, String> {
  /**
   * Checks if both strings belong to the same group.
   *
   * @param a first string
   * @param b second string
   * @param c third string
   * @return true if they do; false otherwise.
   */
  boolean apply(String a, String b, String c);

  @Override default Boolean apply(List<String> items){

    if ((items == null || items.isEmpty())) return false;

    final String a = Iterables.get(items, 0);
    final String b = Iterables.get(items, 1);
    final String c = items.size() == 3 ? Iterables.get(items, 2) : null;

    return apply(a, b, c);
  }
}
