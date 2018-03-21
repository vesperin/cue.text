package com.vesperin.text.groups.kruskal;

import com.vesperin.text.Selection.Document;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Edge implements Comparable<Edge> {

  private final Document v;
  private final Document w;
  private final double weight;
  private final Set<String> labels;

  /**
   * Initializes an edge between vertices <tt>v</tt> and <tt>w</tt> of
   * the given <tt>weight</tt>.
   *
   * @param v      one vertex
   * @param w      the other vertex
   * @param weight the weight of this edge
   */
  Edge(Document v, Document w, double weight) {
    if (Double.isNaN(weight)) {
      throw new IllegalArgumentException("Weight is NaN");
    }

    this.v = v;
    this.w = w;
    this.weight = weight;
    this.labels = new HashSet<>();
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Edge edge = (Edge) o;

    return Double.compare(edge.weight(), weight()) == 0 && !(from() != null ?
      !from().equals(edge.from()) :
      edge.from() != null) && !(to() != null ?
      !to().equals(edge.to()) :
      edge.to() != null);
  }

  @Override public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(weight());
    result = (int) (temp ^ (temp >>> 32));
    result = 31 * result + (from() != null ? from().hashCode() : 0);
    result = 31 * result + (to() != null ? to().hashCode() : 0);
    return result;
  }

  /**
   * Returns the weight of this edge.
   *
   * @return the weight of this edge
   */
  public double weight() {
    return weight;
  }

  /**
   * Returns either endpoint of this edge.
   *
   * @return either endpoint of this edge
   */
  public Document from() {
    return v;
  }

  /**
   * Adds an array of unique labels to this edge.
   *
   * @param values an array of labels that can identify
   *               this edge.
   */
  void labels(String... values) {
    for (String each : values) {
      if (!Objects.isNull(each) && !each.isEmpty()) {
        labels.add(each);
      }
    }
  }

  /**
   * Returns the endpoint of this edge.
   *
   * @return the other endpoint of this edge
   */
  public Document to() {
    return w;
  }

  /**
   * Compares two edges by weight.
   * Note that <tt>compareTo()</tt> is not consistent with <tt>equals()</tt>,
   * which uses the reference equality implementation inherited from <tt>Object</tt>.
   *
   * @param that the other edge
   * @return a negative integer, zero, or positive integer depending on whether
   * the weight of this is less than, equal to, or greater than the
   * argument edge
   */
  @Override public int compareTo(Edge that) {
    return Double.compare(this.weight(), that.weight());
  }

  /**
   * Returns a string representation of this edge.
   *
   * @return a string representation of this edge
   */
  @Override public String toString() {
    return String.format("%s-%s %.5f %s", v.shortName(), w.shortName(), weight(), labels);
  }
}
