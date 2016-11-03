package com.vesperin.text.groups;

import java.util.List;

/**
 * Attracts similar (by some similarity metric) words and
 * puts them into the a group.
 */
public interface Magnet<R, I> {
  R apply(List<I> items);
}
