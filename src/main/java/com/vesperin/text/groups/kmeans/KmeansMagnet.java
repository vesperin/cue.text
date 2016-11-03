package com.vesperin.text.groups.kmeans;

import com.google.common.collect.ImmutableMultiset;
import com.vesperin.text.Grouping;
import com.vesperin.text.Selection;
import com.vesperin.text.groups.Magnet;

import java.util.List;

/**
 * @author Huascar Sanchez
 */
abstract class KmeansMagnet implements Magnet<Grouping.Groups, Selection.Word> {
  public static boolean equals(List<Grouping.VectorGroup> a, List<Grouping.VectorGroup> b) {
    return ImmutableMultiset.copyOf(a).equals(ImmutableMultiset.copyOf(b));
  }
}
