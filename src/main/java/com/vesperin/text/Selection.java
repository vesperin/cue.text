package com.vesperin.text;

import Jama.Matrix;
import com.google.common.primitives.Doubles;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.ProgramUnitLocation;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.SkeletalVisitor;
import com.vesperin.text.nouns.Noun;
import com.vesperin.text.spelling.SpellCorrector;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Jamas;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vesperin.text.spelling.Dictionary.isDefined;
import static com.vesperin.text.spelling.SpellCorrector.containsWord;
import static com.vesperin.text.spelling.SpellCorrector.suggestCorrection;
import static java.util.stream.Collectors.toList;

/**
 * Selection mixin
 *
 * @author Huascar Sanchez
 */
public interface Selection extends Executable {
  /**
   * Creates a word collection strategy that introspects the class name in search for words.
   */
  static WordCollection inspectClassName(Set<StopWords> stopWords){
    return new ClassNameWordCollection(Collections.emptySet(), stopWords);
  }


  /**
   * Creates a word collection strategy that introspects the class name in search for words.
   */
  static WordCollection inspectClassName(Set<String> whiteSet, Set<StopWords> stopWords){
    return new ClassNameWordCollection(whiteSet, stopWords);
  }

  /**
   * Creates a word collection strategy that introspects the method name in search for words.
   */
  static WordCollection inspectMethodName(Set<StopWords> stopWords){
    return new MethodNameWordCollection(Collections.emptySet(), stopWords);
  }

  /**
   * Creates a word collection strategy that introspects the method name in search for words.
   */
  static WordCollection inspectMethodName(Set<String> whiteSet, Set<StopWords> stopWords){
    return new MethodNameWordCollection(whiteSet, stopWords);
  }

  /**
   * Creates a word collection strategy that introspects the entire method body (including
   * signature) in search for words.
   */
  static WordCollection inspectMethodBody(Set<StopWords> stopWords){
    return new MethodBodyWordCollection(Collections.emptySet(), stopWords);
  }

  /**
   * Creates a word collection strategy that introspects the entire method body (including
   * signature) in search for words.
   */
  static WordCollection inspectMethodBody(Set<String> whiteSet, Set<StopWords> stopWords){
    return new MethodBodyWordCollection(whiteSet, stopWords);
  }

  /**
   * Selects the most relevant words in a corpus of source files.
   *
   * @param fromCode corpus
   * @param wordCollection strategy for collecting words in the given code
   * @return a new list of relevant words
   */
  static List<Word> selects(Set<Source> fromCode, WordCollection wordCollection){
    return selects(Integer.MAX_VALUE, fromCode, wordCollection);
  }


  /**
   * Selects the most relevant words in a corpus of source files.
   *
   * @param k limit the list to this number (capped to 10)
   * @param fromCode corpus
   * @return a new list of relevant words
   */
  static List<Word> selects(int k, Set<Source> fromCode, WordCollection wordCollection){
    return new SelectionImpl().weightedWords(k, fromCode, wordCollection);
  }


  /**
   * Creates a new Word object.
   *
   * @param value word's content
   * @return a new Word object.
   */
  static Word createWord(String value){
    return new WordImpl(value);
  }

  /**
   * Catches a list of words from a given source code.
   *
   * @param code Java source file containing source code.
   * @param wordCollection strategy for collecting words in the given code
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(Source code, WordCollection wordCollection) {
    final Context       context = newContext(code);
    final UnitLocation  scope   = buildScope(context);

    if(scope == null) return Collections.emptyList();

    return from(scope, wordCollection);
  }

  /**
   * Catches a list of words from a given located code block.
   *
   * @param scope Block of source code.
   * @param wordCollection strategy for collecting words in the given scope
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(UnitLocation scope, WordCollection wordCollection){
    final Optional<UnitLocation> optional = Optional.ofNullable(scope);

    if(!optional.isPresent()) return Collections.emptyList();

    final UnitLocation  nonNull = optional.get();
    final ASTNode       node    = nonNull.getUnitNode();

    node.accept(wordCollection);

    final List<Word> words = new ArrayList<>(wordCollection.wordList());
    wordCollection.clear();
    return words;
  }

  /**
   * It flattens a list of word duplicates.
   *
   * @param code set of src files
   * @param wordCollection strategy for collecting words in the given code.
   * @return the top k list of words.
   */
  default List<Word> flattenWordList(Set<Source> code, WordCollection wordCollection){
    return frequentWords(Integer.MAX_VALUE, code, wordCollection);
  }

  /**
   * Filters the k most frequent words in the corpus.
   *
   * @param k limit the number words to k words.
   * @param code set of src files
   * @param wordCollection strategy for collecting words in the given code.
   * @return the top k list of words.
   */
  default List<Word> frequentWords(int k, Set<Source> code, WordCollection wordCollection){
    final List<Word> firstPass  = from(code, wordCollection);
    final List<Word> secondPass = cleansing(wordCollection.stopWords(), firstPass);

    return from(secondPass, new WordByFrequency(k));
  }

  /**
   * Filters the list of most relevant words in the existing corpus.
   * Relevancy of a word is determined by a term-frequency-inverse-document-frequency score.
   *
   * @param k limit the number words to k words.
   * @param code the corpus.
   * @return a list of most representative words.
   */
  default List<Word> weightedWords(int k, Set<Source> code, WordCollection wordCollection){
    final List<Word> words = from(flattenWordList(code, wordCollection), new WordByCompositeWeight());
    if(words.isEmpty()) return words;
    final int topK = Math.min(Math.max(0, k), 150);

    return slice(topK, words);
  }

  default List<Word> slice(int k, List<Word> words){
    return words.stream().limit(k).collect(Collectors.toList());
  }


  static List<Word> cleansing(Set<StopWords> stopWords, List<Word> relevant){
    return relevant.stream()
      .filter(w -> !Objects.isNull(w) && !StopWords.isStopWord(stopWords, w.element().toLowerCase(Locale.ENGLISH)))
      .map(w -> Noun.get().isPlural(w.element()) ? WordImpl.from(Noun.get().singularOf(w.element()), w.value(), w.container()) : w )
      .collect(Collectors.toList());
  }


  /**
   * Executes a filter strategy that will return a set of
   * words relevant (by some metric) to the caller of the API.
   *
   * @param words whole list of <code>I</code>s.
   * @param filter filter strategy
   * @param <I> type affected by filter
   * @return a new filtered list of <code>I</code>s.
   */
  default <I> List<I> from(List<I> words, Filter<I> filter){
    return filter.apply(words);
  }

  /**
   * Catches a list of words from a given source code.
   *
   * @param code set of Java source files containing source code.
   * @param wordCollection strategy for collecting words in the given code.
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(Set<Source> code, final WordCollection wordCollection) {
    final List<Word> result = new CopyOnWriteArrayList<>();

    final Collection<Callable<List<Word>>> tasks = new ArrayList<>();
    code.forEach(c -> tasks.add(() -> from(c, wordCollection)));

    final ExecutorService service = scaleExecutor(code.size());

    try {

      final List<Future<List<Word>>> results = service.invokeAll(tasks);
      for (Future<List<Word>> each : results){
        result.addAll(each.get());
      }
    } catch (InterruptedException | ExecutionException e){
      Thread.currentThread().interrupt();
    }

    shutdownService(service);
    return result;
  }


  static UnitLocation buildScope(Context context){
    try {
      return new ProgramUnitLocation(
        context.getCompilationUnit(),
        Locations.locate(context.getCompilationUnit())
      );
    } catch (Exception e){
      System.out.println("Ignoring a package.java class");
      return null;
    }
  }


  /**
   * Parses a source code and returns a new context.
   *
   * @param code the source code to parse
   * @return the parsed context of the source code.
   */
  static Context newContext(Source code){
    Objects.requireNonNull(code);
    return new EclipseJavaParser().parseJava(code);
  }

  interface Filter<I> {
    List<I> apply(List<I> whole);
  }

  interface Word {
    /**
     * Tracks container of word.
     *
     * @param container document containing this word.
     */
    void add(String container);

    /**
     * Counts one step
     */
    default int count() {
      return count(1);
    }

    /**
     * Counts the step
     */
    int count(int step);


    /**
     * @return the current count
     */
    int value();

    /**
     * @return word's container; expressed
     * as path-to-src-file#method-signature signature.
     */
    Set<String> container();


    /**
     * @return word element
     */
    String element();
  }

  interface Document {

    /**
     * Gets the names of all documents in the list of documents.
     *
     * @param documents list of documents to parse
     * @return a list of document names.
     */
    static List<String> names(List<Document> documents){
      return documents.stream().map(Document::path).collect(Collectors.toList());
    }

    /**
     * @return document id
     */
    int id();

    /**
     * @return method name
     */
    String method();

    /**
     * @return the path of document
     */
    String path();

    /**
     * @return the namespace of this document.
     */
    String namespace();

    /**
     * @return the name at the end of the {@link #path()}.
     */
    String shortName();

    /**
     * @return the name transformed for comparison purposes.
     */
    String transformedName();
  }

  class DocumentImpl implements Document {

    final int    id;
    final String filename;
    final String method;
    final String namespace;
    final String shortName;
    final String transformedName;

    public DocumentImpl(int id, String container){
      this.id = id;

      int idx;
      if(!Objects.isNull(container) && container.contains("#")){
        idx = container.lastIndexOf("#");
        this.filename   = container.substring(0, idx);
        this.method     = container.substring(idx + 1, container.length());
        this.namespace  = container.substring(0, idx);
      } else {
        idx = container.lastIndexOf(".");
        this.filename = container;
        this.method   = "";
        this.namespace  = idx > 0 ? container.substring(0, idx) : "";
      }

      // extracts the document's short name
      assert !Objects.isNull(path());
      this.shortName = extractsShortname(path());

      // transforms (if required) this shortname;
      this.transformedName = transforms(shortName());

    }

    private static String extractsShortname(String path){
      final int    index  = path.lastIndexOf(".");
      return (index > 0
        ? path.substring(index + 1, path.length())
        : path);
    }


    private static String transforms(String shortName){

      String   x  = Strings.cleanup(shortName);
      String[] xa = Strings.wordSplit(x);

      if (xa.length == 0) return shortName;

      String xS   = xa[xa.length - 1];
      xS          = Noun.toSingular(xS);

      String lxS  = xS.toLowerCase(Locale.ENGLISH);
      final boolean inTheClub = isDefined(lxS);

      xS = inTheClub ? xS : Strings.firstCharUpperCase(SpellCorrector.suggestCorrection(lxS));

      final StringBuilder sb = new StringBuilder();
      for(int i = 0; i < xa.length - 1; i++){
        sb.append(xa[i]);
      }

      sb.append(xS);

      return sb.toString();
    }

    @Override public boolean equals(Object obj) {
      if(!(obj instanceof Document)) return false;
      final Document doc = (Document) obj;

      return doc.toString().equalsIgnoreCase(toString());
    }

    @Override public int hashCode() {
      return toString().hashCode();
    }

    @Override public int id() {
      return id;
    }

    @Override public String method() {
      return method;
    }

    @Override public String path() {
      return filename;
    }

    @Override public String namespace() {
      return namespace;
    }

    @Override public String shortName() {
      assert !Objects.isNull(path());

      return this.shortName;
    }

    @Override public String transformedName() {
      assert !Objects.isNull(shortName());

      return this.transformedName;
    }

    @Override public String toString() {
      return path() + ("".equals(method()) ? "" :("#" + method()));
    }
  }


  class WordImpl implements Word {
    final String      element;
    final Set<String> container;

    int count;

    WordImpl(String element){
      this.element    = element;
      this.container  = new HashSet<>();
      this.count      = 1;
    }

    static Word from(String element, int count, Set<String> container){
      final WordImpl word = new WordImpl(element);
      word.count = count;

      container.forEach(word::add);

      return word;
    }

    @Override public void add(String container) {
      if(!this.container.contains(container)){
        this.container.add(container);
      }
    }

    @Override public String element() {
      return element;
    }

    @Override public int hashCode() {
      return element().hashCode();
    }

    @Override public boolean equals(Object obj) {
      if(!(obj instanceof Word)) return false;

      final Word other = (Word) obj;

      return other.element().equalsIgnoreCase(element());
    }

    @Override public int count(int step) {
      count = count + step;
      return count;
    }

    @Override public int value() {
      return count;
    }

    @Override public Set<String> container() {
      return container;
    }

    @Override public String toString() {
      return element();
    }
  }

  abstract class WordCollection extends SkeletalVisitor implements Iterable <Word> {
    static final Noun NOUN = Noun.get();

    final List<Word>      items;
    final Set<StopWords>  stopWords;
    final Set<String>     whiteSet;
    final Set<String>     visited;

    WordCollection(Set<String> whiteSet, Set<StopWords> stopWords){
      this.whiteSet   = whiteSet.stream()
        .map(s -> s.toLowerCase(Locale.ENGLISH))
        .collect(Collectors.toSet());

      this.stopWords      = stopWords;
      this.items          = new ArrayList<>();
      this.visited        = new HashSet<>();
    }


    static void addToWordList(String identifier, String container, Set<StopWords> stopWords, List<Word> wordList){
      if(!isThrowableAlike(identifier)){
        // make sure we have a valid split
        String[] split = Strings.wordSplit(identifier);

        for(String eachLabel : Strings.intersect(split, split)){

          if(" ".equals(eachLabel) || eachLabel.isEmpty()
            || StopWords.isStopWord(stopWords, eachLabel, NOUN.pluralOf(eachLabel)))
            continue;

          String currentLabel = eachLabel.toLowerCase(Locale.ENGLISH);

          if(Strings.onlyConsonantsOrVowels(currentLabel) || !containsWord(currentLabel)){
            final String newLabel = suggestCorrection(currentLabel).toLowerCase();

            if(Similarity.jaccard(currentLabel, newLabel) > 0.3D){
              currentLabel = newLabel;
            }
          }

          final String element    = currentLabel.toLowerCase(Locale.ENGLISH);
          final Word   word       = createWord(element);
          word.add(container);

          wordList.add(word);
        }
      }
    }

    void clear(){
      synchronized (this){
        this.visited.clear();
        this.items.clear();
      }
    }

    static boolean isThrowableAlike(String identifier){
      return (identifier.endsWith("Exception")
        || identifier.equals("Throwable")
        || identifier.equals("Error"));
    }

    static boolean isValid(String identifier){
      final String  pattern         = Pattern.quote("_");
      final boolean underscored     = identifier.split(pattern).length == 1;
      final boolean onlyConsonants  = Strings.onlyConsonantsOrVowels(identifier);
      final boolean tooSmall        = identifier.length() < 4;

      return !((underscored && onlyConsonants) || tooSmall);
    }

    static String packageName(TypeDeclaration type){
      assert !Objects.isNull(type);
      assert !Objects.isNull(type.getRoot());

      final CompilationUnit unit = Jdt.parent(CompilationUnit.class, type.getRoot());
      final Optional<PackageDeclaration> op = Optional.ofNullable(unit.getPackage());

      String packageName = "";
      if(op.isPresent()){
        packageName = op.get().getName().getFullyQualifiedName() + ".";
      }

      return packageName;
    }

    static String resolveContainer(SimpleName name){
      final Optional<TypeDeclaration>   type   = Optional.ofNullable(Jdt.parent(TypeDeclaration.class, name));
      final Optional<MethodDeclaration> method = Optional.ofNullable(Jdt.parent(MethodDeclaration.class, name));

      String packageName = "";
      if(type.isPresent()){
        packageName = packageName(type.get());
      }

      final String left  = type.isPresent() ? (packageName + type.get().getName().getFullyQualifiedName()) + (method.isPresent() ? "#" : "") : "";
      final String right = method.isPresent() ? method.get().getName().getIdentifier() + (method.get().isConstructor() ? "(C)" : "") : "";

      return left + right;
    }

    Set<StopWords> stopWords(){
      return stopWords;
    }

    List<Word> wordList(){
      return items;
    }

    @Override public Iterator<Word> iterator() {
      return items.iterator();
    }

  }

  class ClassNameWordCollection extends WordCollection {

    ClassNameWordCollection(Set<String> whiteSet, Set<StopWords> stopWords){
      super(whiteSet, stopWords);
    }


    @Override public boolean visit(TypeDeclaration typeDeclaration) {

      if(!typeDeclaration.isPackageMemberTypeDeclaration()) return true;


      final String identifier = typeDeclaration.getName().getIdentifier();
      final String container  = resolveContainer(typeDeclaration.getName());

      if(visited.contains(container)) return false;
      if(!isValid(identifier)){
        visited.add(container);
        return false;
      }

      addToWordList(identifier, container, stopWords, items);

      visited.add(container);

      return false;
    }
  }

  class MethodNameWordCollection extends WordCollection {
    MethodNameWordCollection(Set<String> whiteSet, Set<StopWords> stopWords){
      super(whiteSet, stopWords);
    }

    @Override public boolean visit(MethodDeclaration methodDeclaration) {

      final SimpleName simpleName = methodDeclaration.getName();
      String methodName = simpleName.getIdentifier();
      methodName        = methodName.toLowerCase(Locale.ENGLISH);
      if(!whiteSet.contains(methodName) && !whiteSet.isEmpty()) return false;

      final String identifier = Strings.trimSideNumbers(simpleName.getIdentifier(), false);

      if(visited.contains(identifier)) return false;
      if(!isValid(identifier)){
        visited.add(identifier);
        return false;
      }


      addToWordList(identifier, resolveContainer(simpleName), stopWords, items);

      visited.add(identifier);

      return false;
    }
  }


  class MethodBodyWordCollection extends WordCollection {
    MethodBodyWordCollection(Set<String> whiteSet, Set<StopWords> stopWords){
      super(whiteSet, stopWords);
    }

    @Override public boolean visit(MethodDeclaration declaration) {

      final Optional<MethodDeclaration> optionalMethod = Optional.ofNullable(declaration);

      if(!optionalMethod.isPresent()) return false;

      final MethodDeclaration method = optionalMethod.get();

      String methodName = method.getName().getIdentifier();
      final String identifier = Strings.trimSideNumbers(methodName, false);
      methodName        = methodName.toLowerCase(Locale.ENGLISH);

      if(!whiteSet.contains(methodName) && !whiteSet.isEmpty()) return false;
      if(visited.contains(identifier)) return false;

      final Optional<Block> optionalBlock = Jdt.getChildren(method).stream()
        .filter(n -> ASTNode.BLOCK == n.getNodeType())
        .map(n -> (Block)n)
        .findFirst();

      if(optionalBlock.isPresent()){

        final Block block = optionalBlock.get();
        final SimpleNameVisitor visitor = new SimpleNameVisitor();
        block.accept(visitor);

        final List<String> ws = visitor.names.stream().map(Strings::wordSplit).flatMap(Arrays::stream).collect(toList());
        final String[] splits = ws.toArray(new String[ws.size()]);

        for(String eachLabel : Strings.intersect(splits, splits)){
          if(" ".equals(eachLabel) || eachLabel.isEmpty()
            || StopWords.isStopWord(stopWords, eachLabel, NOUN.pluralOf(eachLabel)))
            continue;

          String currentLabel = eachLabel.toLowerCase(Locale.ENGLISH);

          if(Strings.onlyConsonantsOrVowels(currentLabel) || !containsWord(currentLabel)){
            final String newLabel = suggestCorrection(currentLabel).toLowerCase();

            if(Similarity.jaccard(currentLabel, newLabel) > 0.3f){
              currentLabel = newLabel;
            }
          }

          final String element    = currentLabel.toLowerCase(Locale.ENGLISH);
          final Word   word       = createWord(element);

          final String container  = resolveContainer(method.getName());
          word.add(container);

          items.add(word);

        }

        visited.add(identifier);
      }


      return false;
    }
  }

  class SimpleNameVisitor extends SkeletalVisitor {
    final Set<String> names = new HashSet<>();
    @Override public boolean visit(SimpleName simpleName) {
      names.add(simpleName.getIdentifier());
      return false;
    }
  }

  class WordCounter {
    private final Set<StopWords>  stopWords;
    private final Map<Word, Word> items;
    private final AtomicInteger   totalItemCount;

    private static final Noun NOUN = Noun.newNoun();


    /**
     * Counts words in some text.
     */
    WordCounter(List<Word> items){
      this(items, EnumSet.of(StopWords.ENGLISH, StopWords.JAVA));
    }

    /**
     * Counts words in some list, paying attention to a set of stop words..
     * @param items items to be counted
     * @param stopWords set of stop words
     */
    private WordCounter(List<Word> items, Set<StopWords> stopWords){
      this.stopWords      = stopWords;
      this.items          = new HashMap<>();
      this.totalItemCount = new AtomicInteger(0);

      addAll(items);
    }

    /**
     * Adds all items in iterable to this counter.
     * @param items iterable made of string items.
     */
    void addAll(final List<Word> items) {
      if(Objects.isNull(items)) return;
      if(items.contains(null))  return;

      for( Word each : items){
        if(Objects.isNull(each)) continue;

        add(each);
      }
    }


    /**
     * Adds an item to this counter.
     *
     * @param item string item.
     */
    void add(Word item) {
      if(item == null) return;

      if(StopWords.isStopWord(stopWords, item.element())) return;

      if(items.containsKey(item)){
        addEntry(item);
      } else {
        final Word singular = singularWord(item);
        if(items.containsKey(singular)){
          if(StopWords.isStopWord(stopWords, singular.element())) return;
          addEntry(singular);
        } else {
          add(item, 1);
        }
      }
    }

    private void addEntry(Word item) {
      final Word entry = items.get(item);
      if(entry == null) {
        add(item, item.count());
      } else {
        entry.container().addAll(item.container());
        add(entry, entry.count());
      }
    }

    /**
     * Adds a fixed count of items to this counter.
     *
     * @param item string item
     * @param count number of times this item will be added.
     */
    void add(Word item, int count) {
      items.put(item, item);
      totalItemCount.addAndGet(count);
    }

    static Word singularWord(Word word){
      final String element = word.element();
      final Set<String> container = word.container();

      final String singElement = NOUN.singularOf(element);
      final Word singular = new WordImpl(singElement);

      container.forEach(singular::add);

      return new WordImpl(singElement);
    }
    /**
     * Returns the list of most frequent items.
     *
     * @param k number of results to collect.
     * @return A list of the min(k, size()) most frequent items
     */
    List<Word> top(int k) {
      final List<Word> all = entriesByFrequency();
      final int resultSize = Math.min(k, items.size());
      final List<Word> result = new ArrayList<>(resultSize);

      result.addAll(all.subList(0, resultSize).stream()
        .collect(Collectors.toList()));

      return Collections.unmodifiableList(result);
    }

    /**
     * Returns the list of items (ordered by their frequency)
     *
     * @return the list of ordered items.
     */
    private List<Word> entriesByFrequency() {
      return items.entrySet().stream()
        .map(Map.Entry::getValue)
        .sorted((a, b) -> Integer.compare(b.value(), a.value()))
        .collect(Collectors.toList());
    }
  }

  class WordByFrequency implements Filter<Word> {
    private final int k;

    WordByFrequency(int k){
      this.k      = k;
    }

    @Override public List<Word> apply(List<Word> words) {
      final WordCounter counter = new WordCounter(words);
      return counter.top(k);
    }
  }

  class WordByCompositeWeight implements Filter<Word> {
    final Index index;

    WordByCompositeWeight(){
      this(new Index());
    }

    WordByCompositeWeight(Index index){
      this.index = index;
    }

    @Override public List<Word> apply(List<Word> words) {
      if(words.isEmpty()) return words;

      index.index(words);
      index.createWordDocMatrix();

      final Map<Word, Double> scores = weightWords(index.wordDocFrequency(), index.wordList());

      final List<Word> allWords = scores.entrySet().stream()
        .sorted((a, b) -> Doubles.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey).collect(toList());

      return allWords.stream().collect(Collectors.toList());
    }

    static Map<Word, Double> weightWords(Matrix raw, List<Word> words) {
      // Turns tf-idf statistic into a score (to be used as word ranking)
      final Matrix tfidf = Jamas.tfidfMatrix(raw);

      final Map<Word, Double> scores = new HashMap<>();
      for (int i = 0; i < tfidf.getRowDimension(); i++) {
        final double s = Jamas.rowSum(tfidf, i);
        scores.put(words.get(i), s);
      }
      return scores;
    }
  }

  class SelectionImpl implements Selection {}

}
