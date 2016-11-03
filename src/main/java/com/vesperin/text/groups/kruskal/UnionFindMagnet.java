package com.vesperin.text.groups.kruskal;

import com.vesperin.text.Grouping;
import com.vesperin.text.Selection;
import com.vesperin.text.groups.Magnet;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public final class UnionFindMagnet implements Magnet<Grouping.Groups, Selection.Document> {

  @Override public Grouping.Groups apply(List<Selection.Document> items) {

    final Graph graph = new Graph(items);
    Graph MST = Kruskal.mst(graph);

    final List<Grouping.Group> clusters = makeClusters(MST);

    return Grouping.Groups.of(clusters);
  }


  private static List<Grouping.Group> makeClusters(Graph graph) {
    final List<Edge> edges = graph.edgeList().stream()
      .collect(Collectors.toList());

    final UnionFind uf = new UnionFind();
    graph.vertexList().forEach(uf::create);

    for (Edge e : edges) {
      uf.union(e.from(), e.to());
    }

    return uf.makeClusters();
  }


}
