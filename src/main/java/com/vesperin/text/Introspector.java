package com.vesperin.text;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.vesperin.base.Source;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.spi.ExecutionMonitor;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Prints;
import com.vesperin.text.utils.Samples;
import com.vesperin.text.utils.Strings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.vesperin.text.Query.Result.items;
import static com.vesperin.text.Query.documents;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * @author Huascar Sanchez
 */
public class Introspector {
  private static final ExecutionMonitor MONITOR = BasicExecutionMonitor.get();

  private Introspector(){}

//  public static void main(String[] args) {
//    final AtomicReference<Source> caught = new AtomicReference<>();
//
//    final JavaParser PARSER = new EclipseJavaParser();
//
//    final Corpus<Source> corpusObject = Corpus.ofSources();
//    final Path start = Paths.get("/Users/hsanchez/Downloads/bigclonebench/dataset/default/");
//
//    corpusObject.addAll(Introspector.from(Ios.collectFiles(start, "java", "package-info")));
//
//    final List<Context> contexts = new ArrayList<>();
//
//    for(Source each : corpusObject){
//      caught.set(each);
//      try {
//        final Context context = PARSER.parseJava(each);
//        contexts.add(context);
//      } catch (Throwable e){
//        System.err.println("File name: " + caught.get().getName());
//        System.err.println((String.format("File %s failed. See: ", caught.get().getName())) + e.getMessage());
//        System.err.println(caught.get().getContent());
//        System.err.println("---------------------------------------------------");
//        caught.set(null);
//      }
//
//    }
//
//    System.out.println(contexts.size() + " parsed.");
//
//  }

  /**
   * Converts a list of files into a list of source objects.
   *
   * @param files the files to be converted
   * @return the list source objects.
   */
  public static List<Source> from(List<File> files) {
    final Predicate<Source> noPackageInfoFiles = s -> !"package-info".equals(s.getName());

    return files.stream()
      .map(Introspector::from)
      .filter(noPackageInfoFiles)
      .collect(Collectors.toList());
  }

  public static Source from(File file) {
    try {
      final String name     = Files.getNameWithoutExtension(file.getName());
      final String content  = Files.readLines(file, Charset.defaultCharset()).stream()
        .collect(Collectors.joining("\n"));

      return Source.from(name, content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates a map consisting of both frequent and typical words.
   *
   * @param fromCorpus corpus object
   * @param tokenizer tokenizes a target body of text from an element in a corpus.
   * @param <T> type of elements in the corpus.
   * @return a mapping between frequent words and typical words.
   */
  @Deprecated public static <T> Map<List<Word>, List<Word>> buildWordsMap(Corpus<T> fromCorpus, WordsTokenizer tokenizer){
    final List<Word> frequentWords = frequentWords(fromCorpus, tokenizer);

    return buildWordsMap(frequentWords.size(), frequentWords);
  }


  /**
   * Generates a map consisting of both frequent and typical words.
   *
   * @param fromTopKWords corpus object
   * @param frequentWords all frequent words in the corpus (sorted already)
   * @return a mapping between frequent words and typical words.
   */
  public static Map<List<Word>, List<Word>> buildWordsMap(int fromTopKWords, List<Word> frequentWords){
    final List<Word>        words   = Lists.newArrayList();
    final Map<String, Word> mapping = Maps.newHashMap();

    List<Word> nonNullFrequentWords = Objects.requireNonNull(frequentWords);

    final int originalCount  = nonNullFrequentWords.size();

    nonNullFrequentWords = nonNullFrequentWords.stream()
      .limit(fromTopKWords)
      .collect(toList());

    MONITOR.info(String.format("Chose top %d words from %d words", fromTopKWords, originalCount));

    nonNullFrequentWords.forEach(w -> {
        mapping.put(w.element(), w);
        words.add(w);
      }
    );


    final List<String> entryList  = words.stream().map(Word::element).collect(toList());

    MONITOR.info(String.format("About to (typicality) rank %d words", entryList.size()));

    final List<String> typicals   = Strings.typicalityRank(entryList);

    MONITOR.info(String.format("Finish (typicality) ranking %d words", entryList.size()));

    final List<Word> typicalWords = typicals.stream()
      .map(mapping::get)
      .collect(Collectors.toList());

    return Collections.singletonMap(frequentWords/*greater than typical ones*/, typicalWords);
  }


  /**
   * TODO
   * @param fromTopKWords
   * @param corpus
   * @param tokenizer
   * @param <T>
   * @return
   */
  public static <T> Groups partitionCorpus(int fromTopKWords, Corpus<T> corpus, WordsTokenizer tokenizer){
    final List<Word> frequentWords = Introspector.frequentWords(corpus, tokenizer);
    final Map<List<Word>, List<Word>> mapping = buildWordsMap(fromTopKWords, frequentWords);

    final List<Word> universe     = Iterables.get(mapping.keySet(), 0);


    if(universe.size() != frequentWords.size()) {
      throw new IllegalStateException("frequentWords and (Words) universe should have same number of elements");
    }

    final List<Word> typicalWords = Iterables.get(mapping.values(), 0);

    final List<Word> typicalitySample   = sampleTypicalWords(typicalWords, universe);

    if(MONITOR.isActive()){ // to avoid the comparison operation
      MONITOR.info("Most frequent words " + Prints.toPrettyPrintedList(universe, true));
      MONITOR.info("Typical words sample " + Prints.toPrettyPrintedList(typicalitySample, false));
    }

    final Query.Result matchedDocuments = documents(typicalWords, universe);

    final List<Document> relevantDocuments = items(matchedDocuments, Document.class);

    MONITOR.info(String.format("Match %d relevant documents", relevantDocuments.size()));

    final Groups groups = Grouping.groupDocs(relevantDocuments);

    MONITOR.info("Begin: Printing groups");
    for(Grouping.Group eachG : groups){
      MONITOR.info(eachG);
    }

    MONITOR.info("End: Printing groups");

    return groups;

  }

  /**
   * Partitions a corpus object into many groups of classes based on the most relevant words in
   * that corpus object.
   *
   * @param corpus the corpus object
   * @param tokenizer the collector of words
   * @param <T> type of elements in the corpus.
   * @return a new Groups object.
   */
  @Deprecated public static <T> Groups partitionCorpus(Corpus<T> corpus, WordsTokenizer tokenizer){

    final Map<List<Word>, List<Word>> mapping = buildWordsMap(corpus, tokenizer);
    if(mapping.isEmpty()) return Groups.emptyGroups();

    final List<Word> universe     = Iterables.get(mapping.keySet(), 0);
    final List<Word> typicalWords = Iterables.get(mapping.values(), 0);

    if(MONITOR.isActive()){ // to avoid the comparison operation
      MONITOR.info("Most frequent words " + Prints.toPrettyPrintedList(
        universe.stream().sorted((a, b) -> Integer.compare(b.count(), a.count())).collect(Collectors.toList()), true)
      );
    }

    final List<Document> relevantDocuments = items(
      documents(
        sampleTypicalWords(
          typicalWords,
          universe),
        universe
      ),
      Document.class
    );

    final Groups groups = Grouping.groupDocs(relevantDocuments);

    MONITOR.info("Begin: Printing groups");
    for(Grouping.Group eachG : groups){
      MONITOR.info(eachG);
    }

    MONITOR.info("End: Printing groups");

    return groups;
  }



  private static List<Word> sampleTypicalWords(List<Word> typical, List<Word> frequent){
    if(typical.size() >= frequent.size()){
      return typical.stream().limit(Samples.chooseK(typical)).collect(toList());
    } else {
      return typical;
    }
  }


  /**
   * Selects the most relevant words in a corpus.
   *
   * @param fromCorpus corpus object
   * @param tokenizer strategy for collecting words in the given corpus
   * @param <T> type elements contained in the corpus.
   * @return a new list of frequent words
   */
  public static <T> List<Word> frequentWords(Corpus<T> fromCorpus, WordsTokenizer tokenizer){

    MONITOR.info(String.format("Getting most frequent words across %d files", fromCorpus.size()));

    List<Word> words = Selection.topKFrequentWords(Integer.MAX_VALUE, fromCorpus, tokenizer);

    // sorts frequent words on ascending order (from hi to lo), by their frequency.
    words = words.stream().sorted((a, b) -> b.count() - a.count()).collect(toList());

    MONITOR.info(String.format("Selection#frequentWords: Got %d words frequent words", words.size()));

    return words;
  }

  /**
   * X. Selects the most typical words in a given corpus.
   *
   * @param k limit the result to the top k most words.
   * @param fromCorpus corpus object.
   * @param tokenizer strategy for collecting words in the given corpus
   * @param <T> type elements contained in the corpus.
   * @return a mapping from frequent words to typical words.
   */
  public static <T> List<Word> typicalityQuery(int k, Corpus<T> fromCorpus, WordsTokenizer tokenizer){
    final List<Word> typicalOnes = typicalityRank(fromCorpus, tokenizer);

    final int topK = Math.min(Math.max(0, k), typicalOnes.size());

    return Selection.slice(topK, typicalOnes);

  }

  /**
   * Ranks the most frequent words in a given corpus by typicality.
   *
   * @param fromCorpus corpus object.
   * @param tokenizer strategy for collecting words in the given corpus
   * @param <T> type elements contained in the corpus.
   * @return a mapping from frequent words to typical words.
   */
  public static <T> List<Word> typicalityRank(Corpus<T> fromCorpus, WordsTokenizer tokenizer){
    final Map<List<Word>, List<Word>> mapping = buildWordsMap(fromCorpus, tokenizer);

    if(mapping.isEmpty()) return Collections.emptyList();

    final List<Word> words = Iterables.get(mapping.values(), 0);

    BasicExecutionMonitor.get().info(String.format(
      "Selection#typicalityRank: Top %d typical words selected ", words.size()
    ));

    return words;
  }

  /**
   * Selects the most representative words in a given corpus.
   *
   * @param typicalList list of typical words
   * @param universeList list of relevant words
   * @param mapping mapping between frequent words and typical words
   * @return a new list of typical words ordered by how representative they are to
   *    the most relevant words in a corpus object. if the size of frequent words is
   *    the same as the size of typical words, then the returned list will be empty.
   */
  public static List<Word> representativeWords(List<Word> typicalList, List<Word> universeList, Map<String, Word> mapping){

    final Set<String> universe = universeList.stream().map(Word::element).collect(toSet());
    final Set<String> typical  = typicalList.stream().map(Word::element).collect(toSet());

    final List<String> representativeOnes = Strings.representativenessRank(typical, universe);
    final List<Word> representative = representativeOnes.stream().map(mapping::get).collect(Collectors.toList());

    if(BasicExecutionMonitor.get().isActive()){
      BasicExecutionMonitor.get().info(String.format(
        "Selection#representativeWords: Top %d representative words selected ", representative.size()
      ));
    }

    return representative;
  }

  /**
   * Finds the most representative words of a corpus object.
   * @param mapping mapping from a list of frequent words to a list of typical words
   * @return list of representative words
   */
  public static List<Word> representativeWords(Map<List<Word>, List<Word>> mapping){
    if(mapping.isEmpty()) return Collections.emptyList();

    final List<Word> frequentOnes = Iterables.get(mapping.keySet(), 0);
    final List<Word> typicalOnes  = sampleTypicalWords(Iterables.get(mapping.values(), 0), frequentOnes);

    return representativeWords(typicalOnes, frequentOnes, indexWords(typicalOnes));
  }

  public static Map<String, Word> indexWords(List<Word> words){
    return words.stream().collect(toMap(Word::element, Function.identity()));
  }

  /**
   * Selects the most representative words in a given corpus.
   *
   * @param fromCorpus corpus object.
   * @param tokenizer strategy for collecting words in the given corpus
   * @param <T> type elements contained in the corpus.
   * @return a new list of typical words ordered by how representative they are to
   *    the most relevant words in a corpus object. if the size of frequent words is
   *    the same as the size of typical words, then the returned list will be empty.
   */
  public static <T> List<Word> representativeWords(Corpus<T> fromCorpus, WordsTokenizer tokenizer){

    final Map<List<Word>, List<Word>> frequentToTypical = buildWordsMap(fromCorpus, tokenizer);
    return representativeWords(frequentToTypical);
  }
}
