package com.vesperin.text;

import com.vesperin.base.Source;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public interface Corpus <T> extends Iterable <T> {

  /**
   * Factory method that produces a corpus of source file elements.
   * @return a new Source Corpus.
   */
  static SourceCorpus ofSources(){
    return new SourceCorpus();
  }

  /**
   * Factory method that produces a corpus of string elements.
   * @return a new String Corpus.
   */
  static StringCorpus ofStrings(){
    return new StringCorpus();
  }

  /**
   * Adds an element to this corpus.
   * @param element corpus element
   */
  default void add(T element){
    dataSet().add(element);
  }

  /**
   * Adds the data set of another corpus to this corpus object.
   * These two corpus object should contain elements of the
   * same kind.
   * @param anotherCorpus the other corpus.
   */
  default void add(Corpus<T> anotherCorpus){
    addAll(anotherCorpus.dataSet());
  }

  /**
   * Adds all elements in a collection to this corpus object.
   * @param elements collection of elements
   */
  default void addAll(Collection<T> elements){
    elements.forEach(this::add);
  }

  /**
   * Clears the corpus.
   */
  default void clear(){
    dataSet().clear();
  }

  /**
   * Checks if the corpus object contains a given element.
   *
   * @param element element to be checked for containment.
   * @return true if the corpus contains this element; false otherwise.
   */
  default boolean contains(T element){
    return dataSet().contains(element);
  }

  /**
   * @return the elements part of this corpus object.
   */
  Set<T> dataSet();

  /**
   * @return the size of this corpus object.
   */
  default int size(){
    return dataSet().size();
  }

  @Override default Iterator<T> iterator() {
    return dataSet().iterator();
  }

  /**
   * Abstract Corpus
   * @param <T> parameter type
   */
  abstract class AbstractCorpus <T> implements Corpus<T>{
    final Set<T> dataSet;

    AbstractCorpus(){
      this(new HashSet<>());
    }

    AbstractCorpus(Set<T> dataSet){
      this.dataSet = dataSet;
    }


    @Override public Set<T> dataSet() {
      return dataSet;
    }

    @Override public String toString() {
      return dataSet().toString();
    }
  }


  class SourceCorpus extends AbstractCorpus <Source> {
    SourceCorpus(){
      super();
    }
  }

  class StringCorpus extends AbstractCorpus <String> {
    StringCorpus(){
      super();
    }
  }
}
