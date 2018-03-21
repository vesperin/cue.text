package com.vesperin.text.groups.kruskal;

import com.google.common.primitives.Doubles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public final class Kruskal {
  private static final Predicate<Edge> SHARED_SUFFIX = (e -> Graph.sharedSuffix(e.from(), e.to()));
  private static final Predicate<Edge> MIN_SCORE  = (e -> Doubles.compare(e.weight(), 0.2D) >= 0);

  /**
   * Creates a minimum spanning tree (MST) from a given {@link Graph}
   *
   * @param graph the given {@link Graph}
   * @return an MST in the form of a {@link Graph}
   */
  public static Graph mst(Graph graph) {
    final UnionFind uf = new UnionFind();
    final List<Edge> edges = new ArrayList<>();


    final List<Edge> filtered = graph.edgeList().stream()
      .filter(MIN_SCORE)
      .filter(SHARED_SUFFIX)
      .collect(Collectors.toList());

    final Graph pre = new Graph(
      graph.vertexList(),
      filtered
    );

    pre.vertexList().forEach(uf::create);
    pre.edgeList().stream().filter(e -> !uf.connected(e.from(), e.to()))
      .forEach(e -> {
        edges.add(e);
        uf.union(e.from(), e.to());
      });


    return new Graph(pre.vertexList(), edges);
  }
}
