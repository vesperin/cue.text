package com.vesperin.text;

import Jama.Matrix;
import com.google.common.primitives.Doubles;
import com.vesperin.base.Context;
import com.vesperin.base.EclipseJavaParser;
import com.vesperin.base.Source;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.locators.ProgramUnitLocation;
import com.vesperin.base.locators.UnitLocation;
import com.vesperin.text.nouns.Noun;
import com.vesperin.text.spelling.SpellCorrector;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.WordsInASTNodeTokenizer;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Jamas;
import com.vesperin.text.utils.Strings;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.vesperin.text.spelling.Dictionary.isDefined;
import static java.util.stream.Collectors.toList;

/**
 * Selection mixin
 * @param <T> type of elements in a {@link Corpus} object.
 * @author Huascar Sanchez
 */
public interface Selection <T> extends Executable {
  /**
   * Selects the most relevant words in a corpus.
   *
   * @param fromCorpus corpus object
   * @param tokenizer strategy for collecting words in the given corpus
   * @param <T> type elements contained in the corpus.
   * @return a new list of relevant words
   */
  static <T> List<Word> selects(Corpus<T> fromCorpus, WordsTokenizer tokenizer){
    return selects(Integer.MAX_VALUE, fromCorpus, tokenizer);
  }


  /**
   * Selects the most relevant words in a corpus.
   *
   * @param k limit the list to this number (capped to 10)
   * @param fromCorpus corpus
   * @param <T> type elements contained in the corpus.
   * @return a new list of relevant words
   */
  static <T> List<Word> selects(int k, Corpus<T> fromCorpus, WordsTokenizer tokenizer){
    return new SelectionImpl<T>().topKWords(k, fromCorpus, tokenizer);
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
   * Selects a list of words from a given element.
   *
   * @param element an element of type {@literal T}
   * @param tokenizer strategy for tokenizing the given element
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(T element, WordsTokenizer tokenizer) {
    final boolean isSource = element instanceof Source;

    if(isSource && !tokenizer.isLightweightTokenizer()){
      final Source        src     = (Source) element;
      final Context       context = newContext(src);
      final UnitLocation  scope   = buildScope(context);

      if(scope == null) return Collections.emptyList();
      return from(scope, tokenizer);
    } else {
      assert element instanceof String;

      final String text = (String) element;
      return from(text, tokenizer);
    }

  }

  /**
   * Catches a list of words from a given text.
   *
   * @param text Java source file containing source text.
   * @param tokenizer strategy for collecting words in the given text
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(String text, WordsTokenizer tokenizer){

    if(text == null || text.isEmpty() || "".equals(text)) return Collections.emptyList();

    final int idx = text.lastIndexOf(".");

    final String identifier = (idx > 0
      ? text.substring(idx, text.length())
      : text
    );

    tokenizer.tokenize(identifier, text);

    final List<Word> words = new ArrayList<>(tokenizer.wordsList());
    tokenizer.clear();

    return words;
  }

  /**
   * Selects a list of words from a given located code block.
   *
   * @param scope Block of source code.
   * @param tokenizer strategy for collecting words in the given scope
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(UnitLocation scope, WordsTokenizer tokenizer){
    final Optional<UnitLocation> optional = Optional.ofNullable(scope);

    if(!optional.isPresent()) return Collections.emptyList();

    final UnitLocation  nonNull = optional.get();
    final ASTNode       node    = nonNull.getUnitNode();

    final WordsInASTNodeTokenizer heavyWeightTokenizer = (WordsInASTNodeTokenizer) tokenizer;
    node.accept(heavyWeightTokenizer);

    final List<Word> words = new ArrayList<>(tokenizer.wordsList());
    tokenizer.clear();
    return words;
  }

  /**
   * It deduplicates a list of words.
   *
   * @param code set of src files
   * @param tokenizer strategy for collecting words in the given code.
   * @return a list of unique words.
   */
  default List<Word> deduplicateWordList(Corpus<T> code, WordsTokenizer tokenizer){
    return frequentWords(Integer.MAX_VALUE, code, tokenizer);
  }

  /**
   * Filters the k most frequent words in the corpus.
   *
   * @param k limit the number words to k words.
   * @param corpus set of elements of type {@literal T}.
   * @param tokenizer strategy for collecting words in the given corpus.
   * @return the top k list of words.
   */
  default List<Word> frequentWords(int k, Corpus<T> corpus, WordsTokenizer tokenizer){
    final List<Word> firstPass  = from(corpus, tokenizer);
    final List<Word> secondPass = cleans(tokenizer.stopWords(), firstPass);

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
  default List<Word> topKWords(int k, Corpus<T> code, WordsTokenizer tokenizer){
    final List<Word> words = from(deduplicateWordList(code, tokenizer), new WordByCompositeWeight());
    if(words.isEmpty()) return words;
    final int topK = Math.min(Math.max(0, k), 150);

    return slice(topK, words);
  }

  default List<Word> slice(int k, List<Word> words){
    return words.stream().limit(k).collect(Collectors.toList());
  }


  static List<Word> cleans(Set<StopWords> stopWords, List<Word> relevant){
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
   * Selects a list of words from a given corpus.
   *
   * @param corpus set of elements of type {@literal T}.
   * @param tokenizer strategy for collecting words in the given corpus.
   * @return a new list of words. Duplicate words are allowed.
   */
  default List<Word> from(Corpus<T> corpus, final WordsTokenizer tokenizer) {
    final List<Word> result = new CopyOnWriteArrayList<>();

    final Collection<Callable<List<Word>>> tasks = new ArrayList<>();
    corpus.forEach(c -> tasks.add(() -> from(c, tokenizer)));

    final ExecutorService service = scaleExecutor(corpus.size());

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

  /**
   * Word type
   */
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

  class SelectionImpl <T> implements Selection <T> {}
}
