package com.vesperin.text;

import Jama.Matrix;
import com.google.common.collect.ImmutableMultiset;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.utils.Jamas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Grouping mixin.
 *
 * @author Huascar Sanchez
 */
public interface Grouping {

  /**
   * Assigns words to specific groups.
   *
   * @param selectedWords relevant words list. See {@link #formWordGroups(List)}
   * @return a new Groups object.
   */
  static Groups formWordGroups(List<Word> selectedWords){
    return new GroupingImpl().wordGroup(selectedWords);
  }

  /**
   * Assigns documents to specific groups.
   *
   * @param selectedWords relevant words list. See {@link #formWordGroups(List)}
   * @return a new Groups object.
   */
  static Groups formDocGroups(List<Word> selectedWords){
    return new GroupingImpl().docGroups(selectedWords);
  }


  /**
   * Groups a list of words using the Kmeans clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
  default Groups wordGroup(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();

    return groups(words, new WordKMeans());
  }

  /**
   * Groups a list of documents containing words that are in input or list of words.
   * Kmeans clustering algorithm is the default clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
  default Groups docGroups(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();

    return groups(words, new DocumentKMeans());
  }

  /**
   * Groups a list of items according to magnetic strategy.
   *
   * @param items items to be grouped
   * @param strategy magnet strategy
   * @param <R> return type
   * @param <I> input type
   * @return a list of wordGroup
   */
  default <R, I> R groups(List<I> items, Magnet<R, I> strategy) {
    return strategy.apply(items);
  }

  /**
   * Attracts similar (by some similarity metric) words and
   * puts them into the a group.
   */
  interface Magnet <R, I> {
    R apply(List<I> items);
  }

  abstract class Kmeans implements Magnet <Groups, Word> {
    static boolean equals(List<Group> a, List<Group> b){
      return ImmutableMultiset.copyOf(a).equals(ImmutableMultiset.copyOf(b));
    }
  }

  class WordKMeans extends Kmeans {

    @Override public Groups apply(List<Word> words) {
      final Index index = Index.createIndex(words);

      final Map<Word, Matrix> wordToMatrix = Jamas.splitMatrix(index.wordList(), index.wordDocFrequency());

      // prelim work
      int numDocs   = words.size();
      int numGroups = (int) Math.floor(Math.sqrt(numDocs));

      final List<Word> initialClusters = new ArrayList<>(numGroups);
      initialClusters.addAll(
        words.stream().limit(numGroups).collect(toList())
      );

      // build initial clusters
      final List<Group> clusters = new ArrayList<>();
      for(int i = 0; i < numGroups; i++){
        final Group cluster = new GroupImpl();
        cluster.add(initialClusters.get(i), wordToMatrix.get(words.get(i)));
        clusters.add(cluster);
      }

      final List<Group> prevClusters = new ArrayList<>();

      while(true){
        int i; for (i = 0; i < numGroups; i++) {
          clusters.get(i).computeCenter();
        }

        for (i = 0; i < numDocs; i++) {
          int bestCluster = 0;
          double maxDistance = Double.MIN_VALUE;
          final Word    word = words.get(i);
          final Matrix  doc  = wordToMatrix.get(word);

          for(int j = 0; j < numGroups; j++){
            final double distance = clusters.get(j).proximity(doc);
            if (distance > maxDistance) {
              bestCluster = j;
              maxDistance = distance;
            }
          }

          clusters.stream()
            .filter(cluster -> cluster.vector(word) != null)
            .forEach(cluster -> cluster.remove(word));

          clusters.get(bestCluster).add(word, doc);
        }

        if(equals(clusters, prevClusters)){
          break;
        }

        prevClusters.clear();
        prevClusters.addAll(clusters);
      }

      return Groups.of(clusters, index);
    }

  }

  class DocumentKMeans extends Kmeans {
    @Override public Groups apply(List<Word> words) {

      final Index index = Index.createIndex(words);

      final List<Document> docList = index.docSet().stream()
        .collect(Collectors.toList());

      final Matrix docToMatrix = index.wordDocFrequency().transpose();
      final Map<Document, Matrix> documents = Jamas.splitMatrix(docList, docToMatrix);

      // prelim work
      int numDocs   = docList.size();
      int numGroups = (int) Math.floor(Math.sqrt(numDocs));

      final List<Document> initialClusters = new ArrayList<>(numGroups);
      initialClusters.addAll(
        docList.stream().limit(numGroups).collect(toList())
      );

      // build initial clusters
      final List<Group> clusters = new ArrayList<>();
      for(int i = 0; i < numGroups; i++){
        final Group cluster = new GroupImpl();
        cluster.add(initialClusters.get(i), documents.get(docList.get(i)));
        clusters.add(cluster);
      }

      final List<Group> prevClusters = new ArrayList<>();
      while(true){
        int i; for (i = 0; i < numGroups; i++) {
          clusters.get(i).computeCenter();
        }

        for (i = 0; i < numDocs; i++) {
          int bestCluster = 0;
          double maxDistance = Double.MIN_VALUE;
          final Document word = docList.get(i);
          final Matrix   doc  = documents.get(word);

          for(int j = 0; j < numGroups; j++){
            final double distance = clusters.get(j).proximity(doc);
            if (distance > maxDistance) {
              bestCluster = j;
              maxDistance = distance;
            }
          }

          clusters.stream()
            .filter(cluster -> cluster.vector(word) != null)
            .forEach(cluster -> cluster.remove(word));

          clusters.get(bestCluster).add(word, doc);
        }

        if(equals(clusters, prevClusters)){
          break;
        }

        prevClusters.clear();
        prevClusters.addAll(clusters);
      }


      return Groups.of(clusters, index);
    }
  }


  /**
   * Group holding a set of similar words (similar by some metric value).
   */
  interface Group extends Iterable <Object> {

    /**
     * Automatically cast the items in the group to their correct type. It will fail fast
     * if trying to cast an item to an incorrect type.
     *
     * @param group formed group of items
     * @param klass target type
     * @param <I> item type
     * @return a new list of items; items were cast to their correct type.
     * @throws ClassCastException if trying to cast an object to a subclass of which it is
     *  not an instance.
     */
    static <I> List<I> items(Group group, Class<I> klass){
      return group.itemList().stream()
        .map(klass::cast).collect(Collectors.toList());
    }

    /**
     * Adds an item and its vector to this group.
     *
     * @param item item object
     * @param vector word vector
     */
    void add(Object item, Matrix vector);

    /**
     * Gets the distance between the computeCenter vector of this group and
     * some other vector.
     *
     * @param toVector vector to compare
     * @return distance between two vectors
     */
    double proximity(Matrix toVector);

    /**
     * The computed centroid of this group.
     *
     * @return the computed centroid.
     */
    Matrix computeCenter();

    /**
     * @return the already computed centroid.
     */
    Matrix center();

    /**
     * @return list of items in group
     */
    List<Object> itemList();

    /**
     * @return true if the group is empty; false otherwise.
     */
    default boolean isEmpty() {
      return itemList().isEmpty();
    }


    @Override default Iterator<Object> iterator() {
      return itemList().iterator();
    }

    /**
     * Removes the item from this group.
     *
     * @param item item to remove
     */
    void remove(Object item);

    /**
     * Gets an item's vector.
     *
     * @param item item object.
     * @return the word vector.
     */
    Matrix vector(Object item);
  }


  /**
   * A Group of words attracted by some magnet strategy.
   */
  class GroupImpl implements Group {
    final Map<Object, Matrix> matrixMap;
    final List<Object>        items;

    Matrix centroid;

    GroupImpl(){
      this.matrixMap = new LinkedHashMap<>();
      this.items     = new LinkedList<>();
      this.centroid  = null;
    }

    @Override public void add(Object item, Matrix vector) {
      matrixMap.put(item, vector);
      itemList().add(item);
    }

    @Override public double proximity(Matrix toDoc) {
      if (centroid != null) {
        return Jamas.computeSimilarity(centroid, toDoc);
      }

      return 0.0D;
    }

    @Override public Matrix computeCenter(){
      if(matrixMap.isEmpty()) return null;

      final Matrix matrix = matrixMap.get(items.get(0));
      centroid = new Matrix(matrix.getRowDimension(), matrix.getColumnDimension());

      for(Object each : matrixMap.keySet()){
        centroid = centroid.plus(matrixMap.get(each));
      }

      centroid = centroid.times(1.0D / matrixMap.size());

      return centroid;
    }

    @Override public Matrix center() {
      return centroid;
    }

    @Override public void remove(Object item) {
      matrixMap.remove(item);
      itemList().remove(item);
    }

    @Override public Matrix vector(Object item){
      return matrixMap.get(item);
    }

    @Override public List<Object> itemList() {
      return items;
    }

    @Override public String toString() {
      return "[" + String.join(", ", itemList().stream().map(Object::toString).collect(toList())) + "]";
    }
  }

  class Groups implements Iterable<Group> {

    final List<Group>  groups;
    final Index        index;

    private Groups(List<Group> groups, Index index){
      this.index  = index;
      this.groups = groups.stream()
        .filter(c -> !c.isEmpty())
        .collect(Collectors.toList());
    }

    /**
     * @return the list of wordGroup produced by {@link Grouping#wordGroup(List)}.
     */
    public List<Group> groupList(){
      return groups;
    }

    /**
     * @return created index.
     */
    public Index index(){
      return index;
    }

    public boolean isEmpty(){
      return groupList().isEmpty();
    }

    static Groups emptyGroups(){
      return of(Collections.emptyList(), new Index());
    }

    static Groups of(List<Group> groups, Index index){
      return new Groups(groups, index);
    }

    @Override public Iterator<Group> iterator() {
      return groupList().iterator();
    }

    @Override public String toString() {
      return groupList().toString();
    }
  }

  class GroupingImpl implements Grouping {}
}
