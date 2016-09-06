package com.vesperin.text.utils;

import com.google.common.collect.ImmutableSet;
import com.vesperin.text.Grouping;
import com.vesperin.text.Selection;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class StringsTest {
  @Test public void testTypicalitySorting() throws Exception {
    final List<String> unsorted = Arrays.asList("Joint", "Joint", "Jo", "Sphere", "Sweep");
    final List<String> sorted   = Strings.typicalitySorting(unsorted.size(), unsorted, ImmutableSet.of("foo"));

    assertEquals("[Joint, Jo, Sphere, Sweep]", sorted.toString());
  }

  @Test public void testGroupsPruning() throws Exception {
    final Grouping.Group  group  = new Grouping.BasicGroup();

    group.add(new Selection.DocumentImpl(1, "org.ode4j.ode.DBox"));
    group.add(new Selection.DocumentImpl(2, "com.jme3.scene.shape.Box"));
    group.add(new Selection.DocumentImpl(3, "net.smert.jreactphysics3d.collision.shapes.BoxShape"));
    group.add(new Selection.DocumentImpl(4, "org.dyn4j.sandbox.SandboxBody"));

    final Grouping.Groups groups = Grouping.refine(Grouping.Groups.of(Collections.singletonList(group)));

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testSandbox() throws Exception {
    assertThat(
      Double.compare(0.4285714285714286, Similarity.damerauLevenshteinScore("Dbox", "Sandbox")) == 0,
      is(true)
    );
  }
}
