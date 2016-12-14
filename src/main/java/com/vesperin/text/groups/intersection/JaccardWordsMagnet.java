package com.vesperin.text.groups.intersection;

import com.vesperin.text.Project;

/**
 * @author Huascar Sanchez
 */
public class JaccardWordsMagnet<T> extends WordsMagnet<T> {

  public JaccardWordsMagnet() {
    super();
  }

  @Override protected double score(Project<T> a, Project<T> b) {
    return jaccard(a, b);
  }

}
