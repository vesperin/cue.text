package com.vesperin.text;

import com.google.common.collect.Iterables;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.spi.ExecutionMonitor;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Prints;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Introspector {
  private static final ExecutionMonitor MONITOR = BasicExecutionMonitor.get();

  private Introspector(){}

  /**
   * Enables execution monitor.
   */
  public static void enableMonitor(){
    MONITOR.enable();
  }

  /**
   * Disables execution monitor.
   */
  public static void disableMonitor(){
    MONITOR.disable();
  }

  /**
   * Finds the most typical words in a corpus object.
   *
   * @param corpus corpus object.
   * @param tokenizer tokenizes target text in the corpus object.
   * @param <T> type of elements in the corpus.
   * @return a new list of words ordered by typicality
   */
  public static <T> List<Word> typicalWords(Corpus<T> corpus, WordsTokenizer tokenizer){
    final List<Word> typicalOnes = Selection.typicalWords(corpus, tokenizer);

    MONITOR.info(("Most typical words " + Prints.toPrettyPrintedList(typicalOnes, false)));

    return typicalOnes;
  }

  /**
   * Finds the most representative words of a corpus object.
   *
   * @param corpus corpus object.
   * @param tokenizer tokenizes a target body of text from an element in a corpus.
   * @param <T> type of elements in the corpus.
   * @return a new list of words ordered by corpus representativeness
   */
  public static <T> List<Word> representativeWords(Corpus<T> corpus, WordsTokenizer tokenizer){
    final List<Word> representativeOnes = Selection.representativeWords(corpus, tokenizer);

    MONITOR.info(
      ("Most representative words " + (representativeOnes.isEmpty()
        ? "No representative words found"
        : Prints.toPrettyPrintedList(representativeOnes, false))
      )
    );

    return representativeOnes;
  }


  /**
   * Generates a mapping between frequent words and typical words.
   *
   * @param corpus corpus object
   * @param tokenizer tokenizes a target body of text from an element in a corpus.
   * @param <T> type of elements in the corpus.
   * @return a mapping between frequent words and typical words.
   */
  public static <T> Map<List<Word>, List<Word>> generateRelevantMapping(Corpus<T> corpus, WordsTokenizer tokenizer){
    return Selection.frequentToTypicalMapping(corpus, tokenizer);
  }

  /**
   * Partitions a corpus object into many groups based on the most relevant words (capped at 150) in
   * that corpus object.
   *
   * @param corpus the corpus object
   * @param tokenizer the collector of words
   * @param <T> type of elements in the corpus.
   * @return a new Groups object.
   */
  public static <T> Groups partitionCorpus(Corpus<T> corpus, WordsTokenizer tokenizer){

    final Map<List<Word>, List<Word>> mapping = generateRelevantMapping(corpus, tokenizer);
    if(mapping.isEmpty()) return Groups.emptyGroups();

    final List<Word> typicalWords = Iterables.get(mapping.keySet(), 0);
    final List<Word> universe     = Iterables.get(mapping.values(), 0);

    MONITOR.info("Most frequent words " + Prints.toPrettyPrintedList(universe.stream().sorted((a, b) -> Integer.compare(b.value(), a.value())).collect(Collectors.toList()), true));

    final List<Document> relevantDocuments = Query.Result.items(
      Query.documents(typicalWords, universe), Document.class
    );

    final Groups groups = Grouping.groupDocs(relevantDocuments);

    MONITOR.info("Begin: Printing groups");
    for(Grouping.Group eachG : groups){
      MONITOR.info(eachG);
    }

    MONITOR.info("End: Printing groups");

    return groups;
  }
}
