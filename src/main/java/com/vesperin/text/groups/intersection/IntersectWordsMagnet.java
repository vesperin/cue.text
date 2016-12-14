package com.vesperin.text.groups.intersection;

import com.google.common.collect.Sets;
import com.vesperin.text.Project;

/**
 * @author Huascar Sanchez
 */
public class IntersectWordsMagnet<T> extends WordsMagnet<T> {

  @Override protected double score(Project<T> a, Project<T> b) {
    return 1.0D * Sets.intersection(a.wordSet(), b.wordSet()).size();
  }
}
