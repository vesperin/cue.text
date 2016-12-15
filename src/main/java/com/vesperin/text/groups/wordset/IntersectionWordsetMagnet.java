package com.vesperin.text.groups.wordset;

import com.google.common.collect.Sets;
import com.vesperin.text.Project;

/**
 * @author Huascar Sanchez
 */
public class IntersectionWordsetMagnet extends WordsetMagnet {

  @Override protected double score(Project a, Project b) {
    return 1.0D * Sets.intersection(a.wordSet(), b.wordSet()).size();
  }
}
