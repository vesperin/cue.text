package com.vesperin.text;

import Jama.Matrix;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.utils.Jamas;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.*;
import java.util.function.Function;
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
   * @param selectedWords relevant words list.
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
   * Assigns documents to specific groups using an MST approach.
   *
   * @param selectedWord most frequent words in some corpus.
   * @return a new Groups object.
   */
  static Groups reformDocGroups(List<Word> selectedWord){
    final Map<Group, Index> mapping = groupIndexMapping(selectedWord);
    return reformDocGroups(mapping);
  }

  /**
   * Creates a mapping from a group to its index.
   *
   * @param selectedWords words to create index and then group.
   * @return a new mapping.
   */
  static Map<Group, Index> groupIndexMapping(List<Word> selectedWords){
    final Index index = new Index();
    index.index(selectedWords);
    index.createWordDocMatrix();

    final Group group = new BasicGroup();
    index.docSet().forEach(group::add);

    return Collections.singletonMap(group, index);
  }

  /**
   * Assigns documents to specific groups using an MST approach.
   *
   * @param input a mapping from group to its index.
   * @return a new Groups object.
   */
  static Groups reformDocGroups(Map<Group, Index> input){
    Objects.requireNonNull(input);
    if(input.isEmpty()) throw new IllegalArgumentException("Empty input");
    if(input.size() > 1) throw new IllegalArgumentException("Not a singleton input");

    final Map.Entry<Group, Index> entry = input.entrySet().stream()
      .findFirst().orElse(null);

    Objects.requireNonNull(entry);

    final Group group   = entry.getKey();
    final Index index   = entry.getValue();
    final Groups groups = new GroupingImpl().reGroups(group);

    return Groups.of(groups, index);
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
   * Performs typicality-based group pruning against a set of groups.
   *
   * @param groups groups before pruning
   * @return groups after pruning
   */
  static Groups prune(Groups groups){

    final List<Group> clusters = new ArrayList<>();

    for(Group each : groups){
      final List<Document> documents = Group.items(each, Document.class);
      final Map<String, Document> mapping = documents.stream()
        .collect(Collectors.toMap(Document::shortName, Function.identity()));

      final List<String> elements = mapping.keySet().stream().collect(Collectors.toList());

      final List<String> words = elements.stream()
        .map(Strings::wordSplit)
        .flatMap(Arrays::stream)
        .collect(Collectors.toList());

      final Map<String, Double> typicalityRegion = Strings.typicalityQuery(words);

      final String typicalWord      = mostTypicalWord(typicalityRegion);
      final double typicalWordScore = typicalityRegion.get(typicalWord);

      // calculates the cluster radius
      final double clusterRadius       = clusterRadius(typicalityRegion, 1.0);
      final Group group = new Grouping.BasicGroup();
      for(String eachElement : elements){

        final String[]    splits  = Strings.wordSplit(eachElement);
        final Set<String> unique  = Strings.intersect(splits, splits);

        for(String eachWord : unique){
          final double score = Math.abs(typicalWordScore - typicalityRegion.get(eachWord));
          if(Doubles.compare(clusterRadius, score) > 0){
            group.add(mapping.get(eachElement));
            break;
          }
        }
      }

      clusters.add(group);
    }

    return Groups.of(clusters);
  }

  static String mostTypicalWord(Map<String, Double> region){
    return region.keySet().stream()
      .sorted((a, b) -> Double.compare(region.get(b), region.get(a)))
      .findFirst().orElse(null);
  }

  static double clusterRadius(Map<String, Double> region, double weight){

    final Set<String> words = region.keySet();

    final double k  = (words.size() * 1.0);
    final double mu = (words.stream().mapToDouble(s -> (region.get(s)))
      .sum())/(words.size());

    final double alpha = words.stream()
      .mapToDouble(
        s -> Math.pow((region.get(s) - mu), 2)
      ).sum();


    return weight * Math.sqrt((1/(k)) * alpha);
  }

  /**
   * Attracts similar (by some similarity metric) words and
   * puts them into the a group.
   */
  interface Magnet <R, I> {
    R apply(List<I> items);
  }

  final class UnionFindClustering implements Magnet <Groups, Document> {

    @Override public Groups apply(List<Document> items) {

      final Graph graph = new Graph(items);
      Graph MST = Kruskal.mst(graph);

      final List<Group> clusters = makeClusters(MST);

      return Groups.of(clusters);
    }


    static List<Group> makeClusters(Graph graph){
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

  final class UnionFind {
    private final Map<Document, List<Document>> childrenMap;
    private final Map<Document, Document>       parentMap;

    /**
     * Creates a UnionFind instance.
     */
    UnionFind(){
      childrenMap = new HashMap<>();
      parentMap   = new HashMap<>();
    }

    /**
     * Used to create a childrenMap for the given {@link Document} and a parentMap
     * for the given {@link Document} in this {@link UnionFind}
     * @param v the given {@link Document}
     */
    void create(Document v) {
      this.childrenMap().put(v, new ArrayList<>(Collections.singletonList(v)));
      this.parentMap().put(v, v);
    }

    /**
     * To find the parent of the given {@link Document} in this {@link UnionFind}
     * @param v the given {@link Document}
     * @return the parent of the given {@link Document} in this {@link UnionFind}
     */
    Document find(Document v) {
      return this.parentMap().get(v);
    }

    boolean connected(Document a, Document b){
      return find(a) == find(b);
    }

    /**
     * To union two given {@link Document}s in this {@link UnionFind}
     * @param a first given {@link Document}
     * @param b second given {@link Document}
     */
    void union(Document a, Document b) {
      Document rentA = this.find(a);
      Document rentB = this.find(b);

      if(Objects.equals(rentA, rentB)) return;

      if (this.childrenMap().get(rentA).size() > this.childrenMap().get(this.find(b)).size()) {
        for (Document v : this.childrenMap().get(rentB)) {

          this.parentMap().put(v, rentA);
          this.childrenMap().get(rentA).add(v);
        }

        this.childrenMap().remove(rentB);
      } else {
        for (Document v : this.childrenMap().get(rentA)) {

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
    Map<Document, List<Document>> childrenMap() {
      return this.childrenMap;
    }

    /**
     * To get the child to parent map of this {@link UnionFind}
     * @return the child to parent map of this {@link UnionFind}
     */
    Map<Document, Document> parentMap() {
      return this.parentMap;
    }

    /**
     * To make the cluster in the form of a group of {@link Document}s
     * @return a cluster in the form of a group of {@link Document}s
     */
    List<Group> makeClusters() {
      final Map<Document, List<Document>> map = new HashMap<>();
      final List<Group> cluster = new ArrayList<>();

      for (Document v : this.parentMap().keySet()) {
        Document rent = this.find(v);
        if (map.containsKey(rent) ) {
          map.get(rent).add(v);
        } else {
          map.put(rent, new ArrayList<>(Collections.singletonList(v)));
        }
      }

      List<Document> entries = map.entrySet().stream()
        .filter(e -> e.getValue().size() == 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());


      // first pass
      updatesMap(entries, map);

      entries = map.entrySet().stream()
        .filter(e -> e.getValue().size() == 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());


      // second pass
      updatesMap(entries, map);

      for(Document each : map.keySet()){
        Group a = new BasicGroup();
        final List<Document> vertices = map.get(each);
        vertices.forEach(a::add);

        cluster.add(a);

      }

      return cluster;
    }

    private static void updatesMap(List<Document> entries, Map<Document, List<Document>> map){
      for(Document orphan : entries){
        Document max = null;

        for(Document parent : map.keySet()){

          if(parent.transformedName().endsWith("Trimesh") && (orphan.transformedName().endsWith("Trimesh"))) {
            System.out.println();
          }

          if (skipPair(orphan, parent)) continue;

          final Set<String> labels = Graph.sharedLabels(orphan, parent);

          if(max == null && (!labels.isEmpty() || Graph.sharedSuffix(orphan, parent))){
            max = parent;
          } else {
            if((Doubles.compare(distance(orphan, max), distance(orphan, parent)) < 0)
              && !labels.isEmpty()){

              max = parent;
            }
          }
        }

        if(max == null) continue;

        map.get(orphan);
        map.get(max).add(orphan);
        map.remove(orphan);
      }
    }


    // only works in re-clustering; trying to match orphan names with potential parents
    private static boolean skipPair(Document orphan, Document parent) {

      final boolean skip = ((Doubles.compare(distance(orphan, parent), 0.6D) < 0)
        && !Graph.sharedSuffix(orphan, parent))
        || Doubles.compare(distance(orphan, parent), distance(orphan, orphan)) == 0;

      final boolean canOverrideSkip = Graph.sharedSuffix(orphan, parent)
        && !orphan.toString().equals(parent.toString());

      return skip && !canOverrideSkip;
    }



    private static double distance(Document a, Document b){
      if(Objects.isNull(b)) return 0.0D;

      String x = a.transformedName(); //Noun.toSingular(Strings.cleanup(a.shortName()));
      String y = b.transformedName(); //Noun.toSingular(Strings.cleanup(b.shortName()));

      return Similarity.normalize(Strings.lcSuffix(x, y), x, y);
    }
  }

  /**
   * Undirected graph.
   */
  final class Graph {
    private final List<Document>  vertices;
    private final List<Edge>      edges;

    /**
     * Constructs an {@link Graph}
     *
     * @param vertices the given {@link Document vertices}
     */
    Graph(List<Document> vertices) {
      this.vertices = vertices;
      this.edges    = makeEdges(this.vertices);
    }

    /**
     * Constructs an {@link Graph}
     *
     * @param vertices the given {@link Document vertices}s
     * @param edges the given {@link Edge}s
     */
    Graph(List<Document> vertices, List<Edge> edges) {
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
    static Set<String> sharedLabels(Document a, Document b){

      String x = a.transformedName(); //Noun.toSingular(a.shortName());
      String y = b.transformedName(); //Noun.toSingular(b.shortName());

      return Strings.intersect(x, y).stream()
        .filter(s -> s.length() >= 3).collect(Collectors.toSet());
    }

    static boolean sharedSuffix(Document a, Document b){

      String x = a.transformedName();
      String y = b.transformedName();

//      String suffix = Strings.sharedSuffix(x, y);
//      return WordCorrector.isValidWord(suffix) && suffix.length() >= 3 && !suffix.equals(suffix.toLowerCase());


//      String x = Strings.cleanup(a.shortName());
//      String y = Strings.cleanup(b.shortName());
//
      String[] xa = Strings.wordSplit(x);
      String[] ya = Strings.wordSplit(y);

      if (xa.length == 0 || ya.length == 0) return false;
//
      String xS   = xa[xa.length - 1];
      String yS   = ya[ya.length - 1];
//
//      xS = Noun.toSingular(xS);
//      yS = Noun.toSingular(yS);
//
//
//      xS = WordCorrector.isValidWord(xS.toLowerCase())
//        ? xS
//        : Strings.firstCharUpperCase(WordCorrector.suggestCorrection(xS.toLowerCase()));
//
//      yS = WordCorrector.isValidWord(yS.toLowerCase())
//        ? yS
//        : Strings.firstCharUpperCase(WordCorrector.suggestCorrection(yS.toLowerCase()));
//
      return xS.equals(yS);
    }

    /**
     * Creates a sorted list of {@link Edge}s
     *
     * @param vertices the given {@link Document vertices}
     * @return a sorted list of {@link Edge}s
     */
    private static List<Edge> makeEdges(List<Document> vertices) {
      final List<Edge> edges = new ArrayList<>();

      for (int i = 0; i < vertices.size(); i++) {
        for (int j = i + 1; j < vertices.size(); j++) {

          final Document a = vertices.get(i);
          final Document b = vertices.get(j);

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


    private static double calculateSuitableDistance(Document a, Document b) throws IllegalStateException {

      String x = a.transformedName();
      String y = b.transformedName();

      return Similarity.lcSuffixScore(x, y);
    }

    List<Edge> edgeList(){
      return edges;
    }

    List<Document> vertexList(){
      return vertices;
    }


  }

  class Edge implements Comparable<Edge> {

    private final Document    v;
    private final Document    w;
    private final double      weight;
    private final Set<String> labels;

    /**
     * Initializes an edge between vertices <tt>v</tt> and <tt>w</tt> of
     * the given <tt>weight</tt>.
     *
     * @param  v one vertex
     * @param  w the other vertex
     * @param  weight the weight of this edge
     */
    Edge(Document v, Document w, double weight) {
      if (Double.isNaN(weight)) {
        throw new IllegalArgumentException("Weight is NaN");
      }

      this.v      = v;
      this.w      = w;
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
    double weight() {
      return weight;
    }

    /**
     * Returns either endpoint of this edge.
     *
     * @return either endpoint of this edge
     */
    Document from() {
      return v;
    }

    /**
     * Adds an array of unique labels to this edge.
     * @param values an array of labels that can identify
     *               this edge.
     */
    void labels(String... values){
      for(String each : values){
        if(!Objects.isNull(each) && !each.isEmpty()){
          labels.add(each);
        }
      }
    }

    /**
     * Returns the endpoint of this edge.
     *
     * @return the other endpoint of this edge
     */
    Document to() {
      return w;
    }

    /**
     * Compares two edges by weight.
     * Note that <tt>compareTo()</tt> is not consistent with <tt>equals()</tt>,
     * which uses the reference equality implementation inherited from <tt>Object</tt>.
     *
     * @param  that the other edge
     * @return a negative integer, zero, or positive integer depending on whether
     *         the weight of this is less than, equal to, or greater than the
     *         argument edge
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
      return String.format("%s-%s %.5f", v.shortName(), w.shortName(), weight());
    }
  }

  /**
   * @author Huascar Sanchez
   */
  final class Kruskal {
    static final Predicate<Edge> SHARED_SUFFIX = (e -> Graph.sharedSuffix(e.from(), e.to()));

    static final Predicate<Edge> MIN_SCORE  = (e -> Doubles.compare(e.weight(), 0.2D) >= 0);

    /**
     * Creates a minimum spanning tree (MST) from a given {@link Graph}
     *
     * @param graph the given {@link Graph}
     * @return an MST in the form of a {@link Graph}
     */
    static Graph mst(Graph graph) {
      final UnionFind uf = new UnionFind();
      final List<Edge> edges = new ArrayList<>();


      final List<Edge> filtered = graph.edgeList().stream()
        .filter(MIN_SCORE)
        .filter(SHARED_SUFFIX)
        .collect(Collectors.toList());

      final Graph pre = new Graph(
        graph.vertexList(),
        filtered
      );

      pre.vertexList().forEach(uf::create);
      pre.edgeList().stream().filter(e -> !uf.connected(e.from(), e.to()))
        .forEach(e -> {
          edges.add(e);
          uf.union(e.from(), e.to());
        });


      return new Graph(pre.vertexList(), edges);
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


    public static Groups of(Groups groups, Index index){
      return of(groups.groupList(), index);
    }

    public static Groups of(List<? extends Group> groups){
      return of(groups, null);
    }

    public static Groups of(List<? extends Group> groups, Index index){
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
