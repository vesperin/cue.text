package com.vesperin.text.utils;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class StringsTest {
  @Test public void testTypicalitySorting() throws Exception {
    final List<String> unsorted = Arrays.asList("Joint", "Joint", "Jo", "Sphere", "Sweep");
    final List<String> sorted   = Strings.typicalitySorting(unsorted.size(), unsorted, ImmutableSet.of("foo"));

    System.out.println(sorted);
  }
}
