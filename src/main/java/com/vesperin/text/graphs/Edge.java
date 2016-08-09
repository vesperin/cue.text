package com.vesperin.text.graphs;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Edge implements Comparable <Edge> {
  final Set<String> labels;
  final Vertex      start;
  final Vertex      end;
  final double      weight;

  /**
   * Constructs a {@link Edge}
   *
   * @param weight weight of this {@link Edge}
   * @param start start {@link Vertex} of this {@link Edge}
   * @param end end {@link Vertex} of this {@link Edge}
   */
  public Edge(double weight, Vertex start, Vertex end) {
    this.weight = weight;
    this.start  = start;
    this.end    = end;
    this.labels = new LinkedHashSet<>();
  }

  public static boolean sameEdge(Edge e, Vertex a, Vertex b){
    return Objects.equals(e, new Edge(e.weight(), a, b));
  }

  void labels(String... values){
    for(String each : values){
      if(!Objects.isNull(each) && !each.isEmpty()){
        labels.add(each);
      }
    }
  }

  @Override public int compareTo(Edge o) {
    return (int) this.weight() - (int) o.weight();
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Edge edge = (Edge) o;

    return Double.compare(edge.weight(), weight()) == 0 && !(start() != null ?
      !start().equals(edge.start()) :
      edge.start() != null) && !(end() != null ?
      !end().equals(edge.end()) :
      edge.end() != null);
  }

  @Override public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(weight());
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + (start() != null ? start().hashCode() : 0);
    result = 31 * result + (end() != null ? end().hashCode() : 0);
    return result;
  }


  public double weight(){
    return weight;
  }

  public Vertex start(){
    return start;
  }

  public Vertex end(){
    return end;
  }

  @Override public String toString() {
    return "Edge{" +
      "weight=" + weight +
      ", start=" + start +
      ", end=" + end +
      '}';
  }

}
