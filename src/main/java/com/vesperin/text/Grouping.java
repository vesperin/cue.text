package com.vesperin.text;

import Jama.Matrix;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.graphs.Edge;
import com.vesperin.text.graphs.UndirectedGraph;
import com.vesperin.text.graphs.Vertex;
import com.vesperin.text.utils.Jamas;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.*;
import java.util.function.Predicate;
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
   * Assigns documents in an existing group to a new set of groups.
   *
   * @param selectedGroup clustered documents.
   * @param cap max size of each group.
   * @return a new Groups object.
   */
  static Groups formDocGroups(Group selectedGroup, int cap){
    if(selectedGroup.size() < cap) return Groups.of(Collections.singletonList(selectedGroup));

    final Predicate<Group> lt  = g -> g.size() < cap;
    final Predicate<Group> gte = g -> g.size() >= cap;

    final Groups gs = new GroupingImpl().reGroups(selectedGroup);

    final List<Group> smallerThanCap = gs.groupList().stream()
      .filter(lt)
      .collect(Collectors.toList());

    final List<Group> greaterThanCap = gs.groupList().stream()
      .filter(gte)
      .collect(Collectors.toList());

    for(Group each : greaterThanCap){
      final Groups regroups = formDocGroups(each);

      for(Group regroup : regroups){
        smallerThanCap.add(regroup);
      }

    }


    return Groups.of(smallerThanCap);
  }


  /**
   * Assigns documents in an existing group to a new set of groups.
   *
   * @param selectedGroup clustered documents.
   * @return a new Groups object.
   */
  static Groups formDocGroups(Group selectedGroup){
    return new GroupingImpl().reGroups(selectedGroup);
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
   * Regroups an existing group based on the longest common sub-sequence metric.
   *
   * @param group group to be regroup-ed
   * @return a new clusters object.
   */
  default Groups reGroups(Group group){
    final List<Document>  docs      = Group.items(group, Document.class);
    return groups(docs, new UnionFindClustering());

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

  class UnionFindClustering implements Magnet <Groups, Document> {

    @Override public Groups apply(List<Document> items) {

      final Set<Document> documents = items.stream().collect(Collectors.toSet());

      final UndirectedGraph graph = new UndirectedGraph(documents);
      UndirectedGraph MST = Kruskal.run(graph);

      final List<Group> clusters = makeClusters(MST);


      return Groups.of(clusters);
    }


    static List<Group> makeClusters(UndirectedGraph graph){
      final List<Edge> edges = graph.edgeList().stream().collect(Collectors.toList());
      final UnionFind uf = new UnionFind();

      graph.vertexList().forEach(uf::create);

      for (Edge e : edges) {
        uf.union(e.start(), e.end());
      }

      return uf.makeClusters();
    }


  }

  final class UnionFind {
    private final Map<Vertex, List<Vertex>> childrenMap = new HashMap<>();
    private final Map<Vertex, Vertex>       parentMap   = new HashMap<>();

    /**
     * Used to create a childrenMap for the given {@link Vertex} and a parentMap
     * for the given {@link Vertex} in this {@link UnionFind}
     * @param v the given {@link Vertex}
     */
    void create(Vertex v) {
      this.childrenMap().put(v, new ArrayList<>(Collections.singletonList(v)));
      this.parentMap().put(v, v);
    }

    /**
     * To find the parent of the given {@link Vertex} in this {@link UnionFind}
     * @param v the given {@link Vertex}
     * @return the parent of the given {@link Vertex} in this {@link UnionFind}
     */
    Vertex find(Vertex v) {
      return this.parentMap().get(v);
    }

    boolean connected(Vertex a, Vertex b){
      return find(a) == find(b);
    }

    /**
     * To union two given {@link Vertex}s in this {@link UnionFind}
     * @param a first given {@link Vertex}
     * @param b second given {@link Vertex}
     */
    void union(Vertex a, Vertex b) {
      Vertex rentA = this.find(a);
      Vertex rentB = this.find(b);

      if(Objects.equals(rentA, rentB)) return;


      if (this.childrenMap().get(rentA).size() > this.childrenMap().get(this.find(b)).size()) {
        for (Vertex v : this.childrenMap().get(rentB)) {
          this.parentMap().put(v, rentA);
          this.childrenMap().get(rentA).add(v);
        }
        this.childrenMap().remove(rentB);
      } else {
        for (Vertex v : this.childrenMap().get(rentA)) {
          this.parentMap().put(v, rentB);
          this.childrenMap().get(rentB).add(v);
        }

        this.childrenMap().remove(rentA);
      }
    }

    /**
     * To get the parent to children map of this {@link UnionFind}
     * @return the parent to children map of this {@link UnionFind}
     */
    Map<Vertex, List<Vertex>> childrenMap() {
      return this.childrenMap;
    }

    /**
     * To get the child to parent map of this {@link UnionFind}
     * @return the child to parent map of this {@link UnionFind}
     */
    Map<Vertex, Vertex> parentMap() {
      return this.parentMap;
    }

    /**
     * To make the cluster in the form of a group of {@link Vertex}s
     * @return a cluster in the form of a group of {@link Vertex}s
     */
    public List<Group> makeClusters() {
      final Map<Vertex, List<Vertex>> map = new HashMap<>();
      final List<Group> cluster = new ArrayList<>();

      for (Vertex v : this.parentMap().keySet()) {
        Vertex rent = this.find(v);
        if (map.containsKey(rent) ) {
          map.get(rent).add(v);
        } else {
          map.put(rent, new ArrayList<>(Collections.singletonList(v)));
        }
      }

      final List<Vertex> entries = map.entrySet().stream()
        .filter(e -> e.getValue().size() == 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());


      for(Vertex orphan : entries){
        Vertex max = null;
        for(Vertex parent : map.keySet()){
          if(Doubles.compare(distance(orphan, parent), 0.5D) < 0)
            continue;
          if(Doubles.compare(distance(orphan, parent), distance(orphan, orphan)) == 0)
            continue;

          if(!shareWords(orphan, parent)) continue;

          if(max == null){
            max = parent;
          } else {
            if(Doubles.compare(distance(orphan, max), distance(orphan, parent)) < 0){
              max = parent;
            }
          }
        }

        if(max == null) continue;

        map.get(orphan);
        map.get(max).add(orphan);
        map.remove(orphan);
      }


      for(Vertex each : map.keySet()){
        Group a = new BasicGroup();
        final List<Vertex> vertices = map.get(each);
        for(Vertex eachV : vertices){
          final Document doc = eachV.data();
          a.add(doc);
        }

        cluster.add(a);

      }

      return cluster;
    }

    private static boolean shareWords(Vertex a, Vertex b){
      final Set<String> labels = Strings.intersect(
        Strings.splits(a.data().shortName()),
        Strings.splits(b.data().shortName())
      );

      return !labels.isEmpty();
    }

    private static double distance(Vertex a, Vertex b){
      return 1.0D - Similarity.lcsSimilarity(a.data().shortName(), b.data().shortName());
    }
  }

  /**
   * @author Huascar Sanchez
   */
  final class Kruskal {
    /**
     * To create a MST from a given {@link UndirectedGraph}
     * @param graph the given {@link UndirectedGraph}
     * @return a MST in the from of a {@link UndirectedGraph}
     */
    static UndirectedGraph run(UndirectedGraph graph) {
      final UnionFind uf = new UnionFind();
      final List<Edge> edges = new ArrayList<>();

      final List<Edge> filtered = graph.edgeList().stream().filter(e -> Doubles.compare(e.weight(), 0.7D) >= 0).collect(Collectors.toList());

      final UndirectedGraph pre = new UndirectedGraph(
        graph.vertexList(),
        filtered
      );

      pre.vertexList().forEach(uf::create);
      pre.edgeList().stream().filter(e -> !uf.connected(e.start(), e.end()))
        .forEach(e -> {
          edges.add(e);
          uf.union(e.start(), e.end());
        });


      return new UndirectedGraph(pre.vertexList(), edges);
    }
  }


  abstract class Kmeans implements Magnet <Groups, Word> {
    static boolean equals(List<VectorGroup> a, List<VectorGroup> b){
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
      final List<VectorGroup> clusters = new ArrayList<>();
      for(int i = 0; i < numGroups; i++){
        final VectorGroup cluster = new VectorGroupImpl();
        cluster.add(initialClusters.get(i), wordToMatrix.get(words.get(i)));
        clusters.add(cluster);
      }

      final List<VectorGroup> prevClusters = new ArrayList<>();

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
      final List<VectorGroup> clusters = new ArrayList<>();
      for(int i = 0; i < numGroups; i++){
        final VectorGroup cluster = new VectorGroupImpl();
        cluster.add(initialClusters.get(i), documents.get(docList.get(i)));
        clusters.add(cluster);
      }

      final List<VectorGroup> prevClusters = new ArrayList<>();
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
     * Automatically cast the items in the group to their correct type. It will fail fast
     * if trying to cast an item to an incorrect type.
     *
     * @param groups formed groups of items
     * @param klass target type
     * @param <I> item type
     * @return a new list of items; items were cast to their correct type.
     * @throws ClassCastException if trying to cast an object to a subclass of which it is
     *  not an instance.
     */
    static <I> Group merge(Class<I> klass, Groups groups){
      final List<I> all = new ArrayList<>();
      for(Group eachGroup : groups){
        if(Objects.isNull(eachGroup)) continue;

        all.addAll(Group.items(eachGroup, klass));

      }

      final BasicGroup group = new BasicGroup();
      group.itemList().addAll(all);
      return group;
    }

    /**
     * Adds an object to this group.
     *
     * @param item item to add
     */
    void add(Object item);

    /**
     * Removes the item from this group.
     *
     * @param item item to remove
     */
    void remove(Object item);

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
     * @return size of the group.
     */
    default int size(){
      return itemList().size();
    }
  }


  /**
   * Group holding a set of similar words (similar by some vector-based metric).
   */
  interface VectorGroup extends Group {
    /**
     * Adds an item to this group.
     *
     * @param item item to add
     */
    default void add(Object item){
      if(!matrixMap().containsKey(item)){
        throw new IllegalStateException("Unable to find item in matrix map");
      }

      itemList().add(item);
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
     * @return the current matrix map.
     */
    Map<Object, Matrix> matrixMap();

    /**
     * Gets an item's vector.
     *
     * @param item item object.
     * @return the word vector.
     */
    Matrix vector(Object item);
  }

  class BasicGroup implements Group {
    final List<Object> items;

    public BasicGroup(){
      this.items = new LinkedList<>();
    }

    @Override public void add(Object item) {
      if(Objects.isNull(item)) return;
      itemList().add(item);
    }

    @Override public boolean equals(Object obj) {
      if(!(obj instanceof Group)) return false;
      final Group objGroup = (Group) obj;
      return objGroup.itemList().equals(itemList());
    }

    @Override public int hashCode() {
      return 31 * itemList().hashCode();
    }

    @Override public void remove(Object item) {
      if(Objects.isNull(item)) return;
      itemList().remove(item);
    }

    @Override public List<Object> itemList() {
      return items;
    }

    @Override public String toString() {
      return itemList().toString();
    }
  }


  /**
   * A Group of words attracted by some magnet strategy.
   */
  class VectorGroupImpl implements VectorGroup {
    final Map<Object, Matrix> matrixMap;
    final Group               impl;

    Matrix centroid;

    VectorGroupImpl(Group impl){
      this.matrixMap = new LinkedHashMap<>();

      this.centroid  = null;
      this.impl      = Objects.requireNonNull(impl);
    }

    VectorGroupImpl(){
      this(new BasicGroup());
    }

    @Override public void add(Object item, Matrix vector) {
      if(!Objects.isNull(vector)){
        matrixMap.put(item, vector);
      }

      add(item);
    }

    @Override public boolean equals(Object obj) {
      return obj instanceof VectorGroup && impl.equals(obj);
    }

    @Override public int hashCode() {
      return impl.hashCode();
    }

    @Override public double proximity(Matrix toDoc) {
      if (centroid != null) {
        return Jamas.computeSimilarity(centroid, toDoc);
      }

      return 0.0D;
    }

    @Override public Matrix computeCenter(){
      if(matrixMap().isEmpty()) return null;

      final Matrix matrix = matrixMap().get(itemList().get(0));
      centroid = new Matrix(matrix.getRowDimension(), matrix.getColumnDimension());

      for(Object each : matrixMap().keySet()){
        centroid = centroid.plus(matrixMap().get(each));
      }

      centroid = centroid.times(1.0D / matrixMap().size());

      return centroid;
    }

    @Override public Matrix center() {
      return centroid;
    }

    @Override public Map<Object, Matrix> matrixMap() {
      return matrixMap;
    }

    @Override public void remove(Object item) {
      if(matrixMap().containsKey(item)) matrixMap().remove(item);
      itemList().remove(item);
    }

    @Override public Matrix vector(Object item){
      return matrixMap.get(item);
    }

    @Override public List<Object> itemList() {
      return impl.itemList();
    }

    @Override public String toString() {
      return "[" + String.join(", ", itemList().stream().map(Object::toString).collect(toList())) + "]";
    }
  }

  class Groups implements Iterable<Group> {

    final List<Group>  groups;
    final Index        index;

    private Groups(List<? extends Group> groups, Index index){
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


    static Groups of(List<? extends Group> groups){
      return of(groups, null);
    }

    static Groups of(List<? extends Group> groups, Index index){
      return new Groups(groups, index);
    }

    @Override public Iterator<Group> iterator() {
      return groupList().iterator();
    }

    public int size(){
      return groupList().size();
    }

    @Override public String toString() {
      return groupList().toString();
    }
  }

  class GroupingImpl implements Grouping {}
}
