package com.vesperin.text.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class SimilarityTest {
  @Test public void testLCSDistance() throws Exception {
    final List<String> a = Arrays.asList("org.dyn4j.dynamics.DetectResult", "org.dyn4j.dynamics.DetectAdapter", "org.dyn4j.dynamics.DetectListener");
    final List<String> b = Collections.singletonList("org.dyn4j.dynamics.DetectBroadphaseFilter");

    for(String s1 : a){
      for(String s2 : b){

        final double distance  = Similarity.lcsDistanceScore(s1, s2);
        final double distance2 = Similarity.lcSubstrScore(s1, s2);

        assertTrue(Double.compare(distance, distance2) < 0.0D);

        System.out.println(String.format("%s and %s", s1, s2) + ": beta = " + distance + "alpha: " + Double.compare(distance, 0.5D) + ", close = " + (Double.compare(distance, 0.5D) < 0));
        System.out.println(String.format("%s and %s", s1, s2) + ": gamma = " + distance2 + ".");
      }
    }
  }
}
