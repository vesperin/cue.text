package com.vesperin.text.spi;

import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Magnets {
  private Magnets(){}

  /**
   * @return a new StringSimilarityMagnet
   */
  public static StringMagnet createStringSimilarityMagnet(){
    return new StringSimilarityMagnet();
  }

  /**
   * @return a new WordIntersectionStringMagnet.
   */
  public static StringMagnet createStringIntersectionMagnet(){
    return new StringIntersectionMagnet();
  }


  private static class StringSimilarityMagnet implements StringMagnet {

    @Override public boolean apply(String a, String b, String c) {
      final double eoDistance = Similarity.jaccardDistance(a, b);
      final double moDistance = Similarity.jaccardDistance(a, c);

      return (Double.compare(eoDistance, 0.5) <= 0) && (Double.compare(eoDistance, moDistance) < 0);
    }
  }

  private static class StringIntersectionMagnet implements StringMagnet {
    @Override public boolean apply(String o1, String o2, String o3) {

      String[] a = Strings.wordSplit(o1);
      String[] b = Strings.wordSplit(o2);
      String[] c = Strings.wordSplit(o3);

      Set<String> i1 = Strings.intersect(a, b);
      Set<String> i2 = Strings.intersect(a, c);

      return (i1.size() > i2.size());

    }
  }
}
