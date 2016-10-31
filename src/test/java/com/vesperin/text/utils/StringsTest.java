package com.vesperin.text.utils;

import com.google.common.collect.Sets;
import com.vesperin.text.Grouping;
import com.vesperin.text.Selection;
import com.vesperin.text.spelling.StopWords;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class StringsTest {
  @Test public void testTypicalitySorting() throws Exception {
    final List<String> unsorted = Arrays.asList("Joint", "Jo", "Join", "Join", "Sphere", "Sweep");
    StopWords stopWords = StopWords.CUSTOM;
    stopWords.add("foo");
    final List<String> sorted   = Strings.typicalityRank(unsorted.size(), unsorted);

    assertEquals("[Join, Joint, Jo, Sphere, Sweep]", sorted.toString());
  }

  @Test public void testGroupsPruning() throws Exception {
    final Grouping.Group  group  = new Grouping.BasicGroup();

    group.add(new Selection.DocumentImpl(1, "org.ode4j.ode.DBox"));
    group.add(new Selection.DocumentImpl(2, "com.jme3.scene.shape.Box"));
    group.add(new Selection.DocumentImpl(3, "net.smert.jreactphysics3d.collision.shapes.BoxShape"));
    group.add(new Selection.DocumentImpl(4, "org.dyn4j.sandbox.SandboxBody"));

    final Grouping.Groups oldGroups = Grouping.Groups.of(Collections.singletonList(group));
    final Grouping.Groups groups    = Grouping.prune(oldGroups);

    assertTrue(!groups.isEmpty());

    final Set<String> oracle = Sets.newHashSet("DBox", "Box", "BoxShape");

    for(Grouping.Group each : groups){
      final Set<String> matches = Grouping.Group.items(each, Selection.Document.class)
        .stream()
        .map(Selection.Document::shortName)
        .collect(Collectors.toSet());

      for(String eachName : matches){
        assertThat(oracle.contains(eachName), is(true));
      }
    }

  }

  @Test public void testSandbox() throws Exception {
    assertThat(
      Double.compare(0.4285714285714286, Similarity.damerauLevenshteinScore("Dbox", "Sandbox")) == 0,
      is(true)
    );
  }
}
