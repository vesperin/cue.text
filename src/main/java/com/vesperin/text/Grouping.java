package com.vesperin.text;

import Jama.Matrix;
import com.google.common.collect.ImmutableMultiset;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.utils.Jamas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
   * @param selectedWords relevant words list. See {@link #formGroups(List)}
   * @return a new Groups.
   */
  static Groups formGroups(List<Word> selectedWords){
    return new GroupingImpl().groups(selectedWords);
  }

  /**
   * Groups a list of words using the Kmeans clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
    default Groups groups(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();

    return groups(words, new KMeans());
  }

  /**
   * Groups a list of items according to magnetic strategy.
   *
   * @param items items to be grouped
   * @param strategy magnet strategy
   * @param <R> return type
   * @param <I> input type
   * @return a list of groups
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

  class KMeans implements Magnet <Groups, Word> {

    @Override public Groups apply(List<Word> words) {
      final Index index = Index.createIndex(words);

      final Map<Word, Matrix> wordToMatrix = splitMatrix(index.wordList(), index.lsiMatrix());

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

    static boolean equals(List<Group> a, List<Group> b){
      return ImmutableMultiset.copyOf(a).equals(ImmutableMultiset.copyOf(b));
    }

    static Map<Word, Matrix> splitMatrix(List<Word> words, Matrix matrix){
      final Map<Word, Matrix> map = new HashMap<>();
      int idx = 0; for (Word each : words){
        map.put(each, Jamas.getRow(matrix, idx));
        idx++;
      }

      return map;
    }
  }

  /**
   * Group holding a set of similar words (similar by some metric value).
   */
  interface Group {
    /**
     * Adds a word and its vector to this group.
     * @param word word object
     * @param vector word vector
     */
    void add(Word word, Matrix vector);

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
     * Removes the word from this group.
     *
     * @param word word to remove
     */
    void remove(Word word);

    /**
     * Gets the word vector.
     *
     * @param word word object.
     * @return the word vector.
     */
    Matrix vector(Word word);

    /**
     * @return list of words in group
     */
    List<Word> wordList();
  }


  /**
   * A Group of words attracted by some magnet strategy.
   */
  class GroupImpl implements Group {
    final Map<Word, Matrix> matrixMap;
    final List<Word>        words;

    Matrix centroid;

    GroupImpl(){
      this.matrixMap = new LinkedHashMap<>();
      this.words     = new LinkedList<>();
      this.centroid  = null;
    }

    @Override public void add(Word word, Matrix vector) {
      matrixMap.put(word, vector);
      wordList().add(word);
    }

    @Override public double proximity(Matrix toDoc) {
      if (centroid != null) {
        return Jamas.computeSimilarity(centroid, toDoc);
      }

      return 0.0D;
    }

    @Override public Matrix computeCenter(){
      if(matrixMap.isEmpty()) return null;

      final Matrix matrix = matrixMap.get(words.get(0));
      centroid = new Matrix(matrix.getRowDimension(), matrix.getColumnDimension());

      for(Word each : matrixMap.keySet()){
        centroid = centroid.plus(matrixMap.get(each));
      }

      centroid = centroid.times(1.0D / matrixMap.size());

      return centroid;
    }

    @Override public Matrix center() {
      return centroid;
    }

    @Override public void remove(Word word) {
      matrixMap.remove(word);
      wordList().remove(word);
    }

    @Override public Matrix vector(Word word){
      return matrixMap.get(word);
    }

    @Override public List<Word> wordList() {
      return words;
    }

    @Override public String toString() {
      return "[" + String.join(", ", wordList().stream()
        .map(Word::element).collect(toList()))
        + "]";
    }
  }

  class Groups implements Iterable<Group> {

    final List<Group>  groups;
    final Index        index;

    private Groups(List<Group> groups, Index index){
      this.index  = index;
      this.groups = groups;
    }

    /**
     * @return the list of groups produced by {@link Grouping#groups(List)}.
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
