package com.vesperin.text;

import Jama.Matrix;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.groups.Magnet;
import com.vesperin.text.groups.kmeans.DocumentKMeans;
import com.vesperin.text.groups.kmeans.WordKMeans;
import com.vesperin.text.groups.kruskal.UnionFindMagnet;
import com.vesperin.text.groups.wordset.IntersectionWordsetMagnet;
import com.vesperin.text.groups.wordset.JaccardWordsetMagnet;
import com.vesperin.text.groups.wordset.WordsetMagnet;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsInASTNodeTokenizer;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Jamas;
import com.vesperin.text.utils.Strings;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Grouping mixin.
 *
 * @author Huascar Sanchez
 */
public interface Grouping extends Executable {

  /**
   * @return a new Basic Group
   */
  static Group newGroup(){
    return new BasicGroup();
  }

  /**
   * @param name name of the group
   * @return a new Named Basic Group
   */
  static Group newNamedGroup(String name){
    return new NamedBasicGroup(name);
  }

  /**
   * @return a new Vector Group
   */
  static VectorGroup newVectorGroup(){
    return new VectorGroupImpl();
  }

  /**
   * Assigns words to specific groups.
   *
   * @param selectedWords relevant words list.
   * @return a new Groups object.
   */
  static Groups groupWords(List<Word> selectedWords){
    return new GroupingImpl().ofWords(selectedWords);
  }

  /**
   * Assigns documents to specific groups.
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
   * Creates a mapping from a group to its index.
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
   * Assigns documents in an existing group to a new set of groups.
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
    return regroups(selectedGroup, 150);
  }


  /**
   * Assigns documents in an existing group to a new set of groups.
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

  static <T> Groups groupDocs(Corpus<T> corpus, WordsTokenizer tokenizer){
    Objects.requireNonNull(corpus);

    final boolean isMethodName  = Tokenizers.isMethodNameTokenizer(tokenizer);
    final boolean isClassName   = Tokenizers.isClassNameTokenizer(tokenizer);


    final Map<String, Set<Document>> clusters = new HashMap<>();
    final WordsInASTNodeTokenizer nonNullTokenizer =
      (WordsInASTNodeTokenizer) Objects.requireNonNull(tokenizer);

    @SuppressWarnings("unchecked")
    final Set<Source>    dataSet      = (Set<Source>) corpus.dataSet(); // unchecked warning
    final List<Source>   sortedCorpus = dataSet.stream()
      .sorted((a, b) -> b.getName().length() - a.getName().length())
      .collect(Collectors.toList());

    for(Source each : sortedCorpus){

      final Source source = Objects.requireNonNull(each);
      final Corpus<Source> singleton = Corpus.ofSources();
      singleton.add(source);

      final Selection<Source> sourceSelection = Selection.newSelection();

      List<Word>  words = sourceSelection.from(singleton, nonNullTokenizer);
      Selection.WordCounter wordCounter = new Selection.WordCounter(words, tokenizer.stopWords());
      words = wordCounter.top(words.size());

      if(words.isEmpty()) continue;

      if(isClassName){

        final Document document = new Selection.DocumentImpl(
          words.get(0).element().hashCode(),
          words.get(0).container().iterator().next()
        );

        trapDocument(document, words, clusters);

      } else if(isMethodName){

        final Index index = Index.createIndex(words);
        final Set<Document> indexedDocuments = index.docSet();

        for(Document document : indexedDocuments) {

          final List<Word> documentWords = index.wordList(document);
          trapDocument(document, documentWords, clusters);
        }
      }

    }

    if(clusters.isEmpty()) return Groups.emptyGroups();

    final List<Group> localGroups = new ArrayList<>();
    for(String label : clusters.keySet()){
      final Group group = Grouping.newNamedGroup(label);

      clusters.get(label).forEach(group::add);

      localGroups.add(group);
    }

    return Groups.of(localGroups);

  }

  static void trapDocument(Document document, List<Word>  words, Map<String, Set<Document>> clusters){

    String key   = makeKey(words);
    String venue = findVenue(clusters, words);
    if(venue.length() == 0) {
      clusters.put(key, new HashSet<>());
    } else {
      key = venue;
    }

    clusters.get(key).add(document);
  }

  static String findVenue(Map<String, Set<Document>> map, List<Word> words){

    final List<String> wordStrings = words.stream().map(Word::element).collect(toList());
    Collections.reverse(wordStrings);

    final  Set<String> memory = new HashSet<>();
    String query  = null;
    String maxKey = "";

    boolean match;

    for(String word : wordStrings){
      if(Objects.isNull(query)) { query = word + ";"; } else {
        query = word + ";" + query;
      }

      if(containsKey(memory, map, query) && query.length() > maxKey.length()){
        maxKey = query;
        match  = true;
      } else {
        match = false;
      }

      if(!match) break;
    }

    return memory.isEmpty() ? maxKey : memory.iterator().next();
  }

  static String longestCommonSuffix(List<String> strings) {
    if (strings.size() == 0) { return null; }

    for (int suffixLen = 0; suffixLen < strings.get(0).length(); suffixLen++) {
      char c = strings.get(0).charAt(suffixLen);
      for (int i = 1; i < strings.size(); i++) {
        if ( suffixLen >= strings.get(i).length() ||
          strings.get(i).charAt(suffixLen) != c ) {
          // Mismatch found
          return strings.get(i).substring(0, suffixLen);
        }
      }
    }
    return strings.get(0);
  }

  static boolean containsKey(Set<String> mem, Map<String, Set<Document>> map, String query){
    for(String eachKey : map.keySet()){
      if(eachKey.endsWith(query)) {
        mem.add(eachKey);
        return true;
      }
    }

    return false;
  }


  static String makeKey(List<Word> words) {

    final StringBuilder sb = new StringBuilder(words.size() * 20);

    for (Word s : words) {
      sb.append(s);
      sb.append(";");
    }

    return sb.toString();
  }

  /**
   * Assigns projects to specific groups of projects using word set intersection.
   *
   * @param projects projects to partition
   * @return a new groups object.
   */
  static Groups groupProjectsBySetIntersection(List<Project> projects){
    return new GroupingImpl().ofProjects(projects, new IntersectionWordsetMagnet());
  }


  /**
   * Assigns projects to specific groups of projects using word set intersection.
   *
   * @param projects projects to partition
   * @return a new groups object.
   */
  static Groups groupProjectsBySetSimilarity(List<Project> projects){
    return new GroupingImpl().ofProjects(projects, new JaccardWordsetMagnet());
  }

  /**
   * Assigns projects to specific groups using the Kmeans algorithm.
   *
   * @param projects projects to be partition.
   * @return a new groups object.
   */
  static Groups groupProjectsByKmeans(List<Project> projects){
    final Map<Word, Set<String>> map = Maps.newHashMap();

    for(Project p : projects){
      final Set<Word> words  = p.wordSet();

      for(Word w : words){

        final Word nWord = Selection.createWord(w.element());
        if(map.containsKey(nWord)){
          map.get(nWord).add(p.name());
        } else {
          map.put(nWord, Sets.newHashSet(p.name()));
        }
      }
    }

    for(Word each : map.keySet()){

      final Set<String> containers = map.get(each);
      containers.forEach(each::add);
    }

    return groupDocsUsingWords(map.keySet().stream().collect(toList()));
  }


  /**
   * Assigns projects to specific groups.
   *
   * @param projects list of projects to partition
   * @param magnet clustering strategy
   * @return a new groups object.
   */
  default Groups ofProjects(List<Project> projects, WordsetMagnet magnet){
    if(Objects.isNull(projects) || projects.isEmpty() || Objects.isNull(magnet))
      return Groups.emptyGroups();

    final Groups groups = groups(projects, magnet);

    BasicExecutionMonitor.get().info(
      String.format(
        "Grouping#ofProjects: %d projects were partitioned into %d clusters; " +
          "using the Typical Words Intersection algorithm.",
        projects.size(),
        groups.size()
      )
    );

    return groups;
  }

  /**
   * Groups a list of words using the Kmeans clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
  default Groups ofWords(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();

    final Groups groups = groups(words, new WordKMeans());

    BasicExecutionMonitor.get().info(
      String.format(
        "Grouping#ofWords: %d words were partitioned into %d clusters; using the Kmeans algorithm.",
        words.size(),
        groups.size()
      )
    );

    return groups;
  }

  /**
   * Groups a list of documents containing words that are in input or list of words.
   * Kmeans clustering algorithm is the default clustering algorithm.
   *
   * @param words a non empty list of words to be clustered.
   * @return a list of clusters.
   */
  default Groups ofDocs(List<Word> words) {
    if(Objects.isNull(words) || words.isEmpty())
      return Groups.emptyGroups();


    final Groups groups = groups(words, new DocumentKMeans());

    if(BasicExecutionMonitor.get().isActive()){

      final Index index = new Index();
      index.index(words);

      BasicExecutionMonitor.get().info(
        String.format(
          "Grouping#ofDocs: %d documents were partitioned into %d clusters; using the Kmeans algorithm.",
          index.docSet().size(),
          groups.size()
        )
      );
    }

    return groups;
  }

  /**
   * Regroups an existing group based on the longest common sub-sequence metric.
   *
   * @param group group to be regroup-ed
   * @return a new clusters object.
   */
  default Groups reGroups(Group group){
    final List<Document>  docs  = Group.items(group, Document.class);
    final Groups groups = groups(docs, new UnionFindMagnet());


    BasicExecutionMonitor.get().info(
      String.format(
        "Grouping#reGroups: %d documents were partitioned into %d clusters; using the Kruskal algorithm.",
        docs.size(),
        groups.size()
      )
    );

    return groups;

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

  interface NamedGroup extends Group {
    /**
     * @return the name of this group
     */
    String name();
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
      final Set<Object> o = objGroup.itemList().stream().collect(Collectors.toSet());
      final Set<Object> i = itemList().stream().collect(Collectors.toSet());
      return o.equals(i);
    }

    @Override public int hashCode() {
      return 31 * itemList().stream().collect(toSet()).hashCode();
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

  class NamedBasicGroup implements NamedGroup {
    private final String  name;
    private final Group   impl;

    NamedBasicGroup(String name){
      this(name, new BasicGroup());
    }

    NamedBasicGroup(String name, Group impl){
      this.name = name;
      this.impl = impl;
    }

    @Override public void add(Object item) {
      impl.add(item);
    }

    @Override public void remove(Object item) {
      impl.remove(item);
    }

    @Override public List<Object> itemList() {
      return impl.itemList();
    }

    @Override public String name() {
      return name;
    }

    @Override public String toString() {
      return name() + ":" + impl.toString();
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
