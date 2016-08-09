package com.vesperin.text.graphs;

import com.vesperin.text.Selection.Document;

/**
 * @author Huascar Sanchez
 */
public class Vertex {
  private final Document data;

  /**
   * Construct a new vertex with a given data.
   *
   * @param data given data.
   */
  public Vertex(Document data){
    this.data = data;
  }

  /**
   * @return stored data.
   */
  public Document data(){
    return data;
  }

  @Override public int hashCode() {
    return data().hashCode();
  }

  @Override public boolean equals(Object obj) {
    return obj instanceof Vertex && data().equals(((Vertex)obj).data());
  }

  @Override public String toString() {
    return "Vertex { data = " + data() + " }";
  }
}
