package com.vesperin.text.groups.wordset;

import com.vesperin.text.Project;

/**
 * @author Huascar Sanchez
 */
public class JaccardWordsetMagnet extends WordsetMagnet {

  public JaccardWordsetMagnet() {
    super();
  }

  @Override protected double score(Project a, Project b) {
    return jaccard(a, b);
  }

}
