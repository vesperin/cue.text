package com.vesperin.text;

import Jama.Matrix;
import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.groups.Magnet;
import com.vesperin.text.groups.kmeans.DocumentKMeans;
import com.vesperin.text.groups.kmeans.WordKMeans;
import com.vesperin.text.groups.kruskal.UnionFindMagnet;
import com.vesperin.text.utils.Jamas;
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
   * @return a new Basic Group
   */
  static Group newGroup(){
    return new BasicGroup();
  }

  /**
   * @return a new Vector Group
   */
  static VectorGroup newVectorGroup(){
    return new VectorGroupImpl();
  }

  /**
   * X-Assigns words to specific groups.
   *
   * @param selectedWords relevant words list.
   * @return a new Groups object.
   */
  static Groups groupWords(List<Word> selectedWords){
    return new GroupingImpl().ofWords(selectedWords);
  }

  /**
   * X-Assigns documents to specific groups.
   *
   * @param selectedWords relevant words list. See {@link #groupWords(List)}
   * @return a new Groups object.
   */
  static Groups groupDocsUsingWords(List<Word> selectedWords){
    return new GroupingImpl().ofDocs(selectedWords);
  }

  /**
   * Assigns documents to specific groups using an MST approach.
   *
   * @param selectedWord most frequent words in some corpus.
   * @return a new Groups object.
   */
  static Groups regroupDocs(List<Word> selectedWord){
    final Map<Group, Index> mapping = buildGroupIndexMapping(selectedWord);
    return regroupDocs(mapping);
  }

  /**
   * Assigns documents to specific groups using an MST approach.
   *
   * @param input a mapping from group to its index.
   * @return a new Groups object.
   */
  static Groups regroupDocs(Map<Group, Index> input){
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
   * X-Creates a mapping from a group to its index.
   *
   * @param selectedWords words to create index and then group.
   * @return a new mapping.
   */
  static Map<Group, Index> buildGroupIndexMapping(List<Word> selectedWords){
    final Index index = new Index();
    index.index(selectedWords);
    index.createWordDocMatrix();

    final Group group = newGroup();
    index.docSet().forEach(group::add);

    return Collections.singletonMap(group, index);
  }

  /**
   * X-Assigns documents in an existing group to a new set of groups.
   *
   * @param selectedGroup clustered documents.
   * @param cap max size of each group.
   * @return a new Groups object.
   */
  static Groups regroups(Group selectedGroup, int cap){
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
      final Groups regroups = regroups(each);

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
  static Groups regroups(Group selectedGroup){
    return regroups(selectedGroup, 20);
  }


  /**
   * X-Assigns documents in an existing group to a new set of groups.
   *
   * @param documents list of documents.
   * @return a new Groups object.
   */
  static Groups groupDocs(List<Document> documents){
    Objects.requireNonNull(documents);

    final Group group = newGroup();
    documents.forEach(group::add);

    return regroups(group);
  }


  /**
   * X-Groups a list of words using the Kmeans clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
  default Groups ofWords(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();

    return groups(words, new WordKMeans());
  }

  /**
   * X-Groups a list of documents containing words that are in input or list of words.
   * Kmeans clustering algorithm is the default clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
  default Groups ofDocs(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();

    return groups(words, new DocumentKMeans());
  }

  /**
   * X-Regroups an existing group based on the longest common sub-sequence metric.
   *
   * @param group group to be regroup-ed
   * @return a new clusters object.
   */
  default Groups reGroups(Group group){
    final List<Document>  docs  = Group.items(group, Document.class);
    return groups(docs, new UnionFindMagnet());

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
  static Groups pruneDocGroups(Groups groups){

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
      final Group group = Grouping.newGroup();
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

      final Group group = newGroup();
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
     * @return the list of wordGroup produced by {@link Grouping#ofWords(List)}.
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
