package com.vesperin.text.groups.kruskal;

import com.google.common.primitives.Doubles;
import com.vesperin.text.Grouping;
import com.vesperin.text.Selection;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public final class UnionFind {
  private final Map<Selection.Document, List<Selection.Document>> childrenMap;
  private final Map<Selection.Document, Selection.Document> parentMap;

  /**
   * Creates a UnionFind instance.
   */
  public UnionFind() {
    childrenMap = new HashMap<>();
    parentMap = new HashMap<>();
  }

  /**
   * Used to create a childrenMap for the given {@link Selection.Document} and a parentMap
   * for the given {@link Selection.Document} in this {@link UnionFind}
   *
   * @param v the given {@link Selection.Document}
   */
  public void create(Selection.Document v) {
    this.childrenMap().put(v, new ArrayList<>(Collections.singletonList(v)));
    this.parentMap().put(v, v);
  }

  /**
   * To find the parent of the given {@link Selection.Document} in this {@link UnionFind}
   *
   * @param v the given {@link Selection.Document}
   * @return the parent of the given {@link Selection.Document} in this {@link UnionFind}
   */
  private Selection.Document find(Selection.Document v) {
    return this.parentMap().get(v);
  }

  public boolean connected(Selection.Document a, Selection.Document b) {
    return find(a) == find(b);
  }

  /**
   * To union two given {@link Selection.Document}s in this {@link UnionFind}
   *
   * @param a first given {@link Selection.Document}
   * @param b second given {@link Selection.Document}
   */
  public void union(Selection.Document a, Selection.Document b) {
    Selection.Document rentA = this.find(a);
    Selection.Document rentB = this.find(b);

    if (Objects.equals(rentA, rentB)) return;

    if (this.childrenMap().get(rentA).size() > this.childrenMap().get(this.find(b)).size()) {
      for (Selection.Document v : this.childrenMap().get(rentB)) {

        this.parentMap().put(v, rentA);
        this.childrenMap().get(rentA).add(v);
      }

      this.childrenMap().remove(rentB);
    } else {
      for (Selection.Document v : this.childrenMap().get(rentA)) {

        this.parentMap().put(v, rentB);
        this.childrenMap().get(rentB).add(v);

      }

      this.childrenMap().remove(rentA);
    }
  }

  /**
   * To get the parent to children map of this {@link UnionFind}
   *
   * @return the parent to children map of this {@link UnionFind}
   */
  private Map<Selection.Document, List<Selection.Document>> childrenMap() {
    return this.childrenMap;
  }

  /**
   * To get the child to parent map of this {@link UnionFind}
   *
   * @return the child to parent map of this {@link UnionFind}
   */
  private Map<Selection.Document, Selection.Document> parentMap() {
    return this.parentMap;
  }

  /**
   * To make the cluster in the form of a group of {@link Selection.Document}s
   *
   * @return a cluster in the form of a group of {@link Selection.Document}s
   */
  public List<Grouping.Group> makeClusters() {
    final Map<Selection.Document, List<Selection.Document>> map = new HashMap<>();
    final List<Grouping.Group> cluster = new ArrayList<>();

    for (Selection.Document v : this.parentMap().keySet()) {
      Selection.Document rent = this.find(v);
      if (map.containsKey(rent)) {
        map.get(rent).add(v);
      } else {
        map.put(rent, new ArrayList<>(Collections.singletonList(v)));
      }
    }

    List<Selection.Document> entries = map.entrySet().stream()
      .filter(e -> e.getValue().size() == 1)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());


    // first pass
    updatesMap(entries, map);

    entries = map.entrySet().stream()
      .filter(e -> e.getValue().size() == 1)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());


    // second pass
    updatesMap(entries, map);

    for (Selection.Document each : map.keySet()) {
      Grouping.Group a = Grouping.newGroup();
      final List<Selection.Document> vertices = map.get(each);
      vertices.forEach(a::add);

      cluster.add(a);

    }

    return cluster;
  }

  private static void updatesMap(List<Selection.Document> entries, Map<Selection.Document, List<Selection.Document>> map) {
    for (Selection.Document orphan : entries) {
      Selection.Document max = null;

      for (Selection.Document parent : map.keySet()) {

        if (parent.transformedName().endsWith("Trimesh") && (orphan.transformedName().endsWith("Trimesh"))) {
          System.out.println();
        }

        if (skipPair(orphan, parent)) continue;

        final Set<String> labels = Graph.sharedLabels(orphan, parent);

        if (max == null && (!labels.isEmpty() || Graph.sharedSuffix(orphan, parent))) {
          max = parent;
        } else {
          if ((Doubles.compare(distance(orphan, max), distance(orphan, parent)) < 0)
            && !labels.isEmpty()) {

            max = parent;
          }
        }
      }

      if (max == null) continue;

      map.get(orphan);
      map.get(max).add(orphan);
      map.remove(orphan);
    }
  }


  // only works in re-clustering; trying to match orphan names with potential parents
  private static boolean skipPair(Selection.Document orphan, Selection.Document parent) {

    final boolean skip = ((Doubles.compare(distance(orphan, parent), 0.6D) < 0)
      && !Graph.sharedSuffix(orphan, parent))
      || Doubles.compare(distance(orphan, parent), distance(orphan, orphan)) == 0;

    final boolean canOverrideSkip = Graph.sharedSuffix(orphan, parent)
      && !orphan.toString().equals(parent.toString());

    return skip && !canOverrideSkip;
  }


  private static double distance(Selection.Document a, Selection.Document b) {
    if (Objects.isNull(b)) return 0.0D;

    String x = a.transformedName(); //Noun.toSingular(Strings.cleanup(a.shortName()));
    String y = b.transformedName(); //Noun.toSingular(Strings.cleanup(b.shortName()));

    return Similarity.normalize(Strings.lcSuffix(x, y), x, y);
  }
}
