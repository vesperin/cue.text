package com.vesperin.text.groups.kruskal;

import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Undirected graph.
 */
public final class Graph {
  private final List<Selection.Document> vertices;
  private final List<Edge>      edges;

  /**
   * Constructs an {@link Graph}
   *
   * @param vertices the given {@link Selection.Document vertices}
   */
  public Graph(List<Selection.Document> vertices) {
    this.vertices = vertices;
    this.edges    = makeEdges(this.vertices);
  }

  /**
   * Constructs an {@link Graph}
   *
   * @param vertices the given {@link Selection.Document vertices}s
   * @param edges the given {@link Edge}s
   */
  public Graph(List<Selection.Document> vertices, List<Edge> edges) {
    this.vertices = vertices;
    this.edges    = edges;
  }

  /**
   * Intersects shared words between vertices.
   *
   * @param a first vertex
   * @param b second vertex
   * @return intersecting words.
   */
  public static Set<String> sharedLabels(Selection.Document a, Selection.Document b){

    String x = a.transformedName(); //Noun.toSingular(a.shortName());
    String y = b.transformedName(); //Noun.toSingular(b.shortName());

    return Strings.intersect(x, y).stream()
      .filter(s -> s.length() >= 3).collect(Collectors.toSet());
  }

  public static boolean sharedSuffix(Selection.Document a, Selection.Document b){

    String x = a.transformedName();
    String y = b.transformedName();

    String[] xa = Strings.wordSplit(x);
    String[] ya = Strings.wordSplit(y);

    if (xa.length == 0 || ya.length == 0) return false;

    String xS   = xa[xa.length - 1];
    String yS   = ya[ya.length - 1];

    return xS.equals(yS);
  }

  /**
   * Creates a sorted list of {@link Edge}s
   *
   * @param vertices the given {@link Selection.Document vertices}
   * @return a sorted list of {@link Edge}s
   */
  private static List<Edge> makeEdges(List<Selection.Document> vertices) {
    final List<Edge> edges = new ArrayList<>();

    for (int i = 0; i < vertices.size(); i++) {
      for (int j = i + 1; j < vertices.size(); j++) {

        final Selection.Document a = vertices.get(i);
        final Selection.Document b = vertices.get(j);

        final double distance = calculateSuitableDistance(a, b);

        // skip documents with same fully qualified name
        if(a.toString().equals(b.toString()))   continue;

        final Set<String> labels = sharedLabels(a, b);

        // filter to reduce search space

        final boolean sharedSuffix = sharedSuffix(a, b);

        // (distance, k-words)-filter.
        final boolean dissatisfiedDistanceKWordsFilter = (Double.compare(distance, 0.6D) <= 0
          && labels.size() < 2);
        // don't share labels even after spell correction
        //final boolean invalidScenario                  = !validScenario(a.transformedName(), b.transformedName());
        // don't share labels, period.
        final boolean disjointLabels                   = labels.isEmpty();

        if (!sharedSuffix && (dissatisfiedDistanceKWordsFilter || disjointLabels)){
          continue;
        }

        final Edge e = new Edge(a, b, (distance));
        labels.forEach(e::labels);

        e.labels(Strings.sharedSuffix(a.transformedName(), b.transformedName()));

        edges.add(e);
      }
    }

    return edges.stream()
      .sorted((a, b) -> Doubles.compare(b.weight(), a.weight()))
      .collect(Collectors.toList());
  }


  private static double calculateSuitableDistance(Selection.Document a, Selection.Document b) throws IllegalStateException {

    String x = a.transformedName();
    String y = b.transformedName();

    return Similarity.lcSuffixScore(x, y);
  }

  public List<Edge> edgeList(){
    return edges;
  }

  public List<Selection.Document> vertexList(){
    return vertices;
  }
}
