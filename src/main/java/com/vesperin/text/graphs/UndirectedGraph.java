package com.vesperin.text.graphs;

import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class UndirectedGraph {
  private final List<Vertex> vertices;
  private final List<Edge> edges;

  /**
   * Constructs an {@link UndirectedGraph}
   *
   * @param documents the given {@link Document}s
   */
  public UndirectedGraph(Set<Document> documents){
    this(makeVertices(documents));
  }

  /**
   * Constructs an {@link UndirectedGraph}
   *
   * @param vertices the given {@link Vertex}s
   */
  public UndirectedGraph(List<Vertex> vertices) {
    this.vertices = vertices;
    this.edges    = makeEdges(this.vertices);
  }

  /**
   * Constructs an {@link UndirectedGraph}
   *
   * @param vertices the given {@link Vertex}s
   * @param edges the given {@link Edge}s
   */
  public UndirectedGraph(List<Vertex> vertices, List<Edge> edges) {
    this.vertices = vertices;
    this.edges    = edges;
  }

  /**
   * To create a sorted list of {@link Edge}s
   * @param vertices the given {@link Vertex}s
   * @return a sorted list of {@link Edge}s
   */
  private static List<Edge> makeEdges(List<Vertex> vertices) {
    final List<Edge> edges = new ArrayList<>();

    for (int i = 0; i < vertices.size(); i++) {
      for (int j = i + 1; j < vertices.size(); j++) {

        final Vertex a = vertices.get(i);
        final Vertex b = vertices.get(j);

        final double distance = distance(a, b);
        // skip same node
        if(Double.isNaN(distance) || Double.compare(distance, 0D) == 0)   continue;

        final Set<String> labels = Strings.intersect(
          Strings.splits(a.data().shortName()),
          Strings.splits(b.data().shortName())
        );

        // filter to reduce search space
        // (distance, k-words)-filter.
        if(Double.compare(distance, 0.45D) >= 0 && labels.size() < 2) continue;

        final Edge e = new Edge((1.0D - distance), a, b);
        labels.forEach(e::labels);

        edges.add(e);
      }
    }

    return edges.stream()
      .sorted((a, b) -> Doubles.compare(b.weight(), a.weight()))
      .collect(Collectors.toList());
  }

  private static List<Vertex> makeVertices(Set<Document> documents){

    final List<Vertex> vertices = new ArrayList<>();
    for(Document each : documents){
      final Vertex v = new Vertex(each);
      vertices.add(v);
    }

    return vertices;
  }

  private static double distance(Vertex a, Vertex b) throws IllegalStateException {
    return Similarity.lcsSimilarity(a.data().shortName(), b.data().shortName());
  }

  /**
   * To get the {@link Vertex}s of this {@link UndirectedGraph}
   * @return the {@link Vertex}s of this {@link UndirectedGraph}
   */
  public List<Vertex> vertexList() {
    return this.vertices;
  }

  /**
   * To get the {@link Edge}s of this {@link UndirectedGraph}
   * @return the {@link Edge}s of this {@link UndirectedGraph}
   */
  public List<Edge> edgeList() {
    return this.edges;
  }
}
