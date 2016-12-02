package com.vesperin.text;

import Jama.Matrix;
import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.utils.Jamas;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Query mixin
 *
 * @author Huascar Sanchez
 */
public interface Query {
  /**
   * Finds interesting documents in some index using a list of keywords.
   *
   * @param words query of keywords
   * @param index existing index
   * @return a new query result object.
   */
  static Result documents(List<Word> words, Index index){
    Objects.requireNonNull(index);
    Objects.requireNonNull(words);

    final Result result = createQuery().documentSearch(words, index);

    if(BasicExecutionMonitor.get().isActive()){

      BasicExecutionMonitor.get().info(
        String.format(
          "Query#documents: %d documents sharing these %s words.",
          result.documents.size(),
          words
        )
      );

    }

    return result;
  }

  /**
   * Finds interesting words in a list of fully.qualified.ClassName[#methodName]
   * entries (a.k.a. documents).
   *
   * @param docs query of documents
   * @param index existing index
   * @return a new query result object.
   */
  static Result words(List<Document> docs, Index index){
    Objects.requireNonNull(index);
    Objects.requireNonNull(docs);

    final Result result = createQuery().wordSearch(docs, index);

    if(BasicExecutionMonitor.get().isActive()){

      BasicExecutionMonitor.get().info(
        String.format(
          "Query#words: %d words found in these %s documents.",
          result.documents.size(),
          docs
        )
      );

    }


    return result;
  }

  /**
   * Finds interesting fully.qualified.ClassName[#methodName] entries (Documents) in
   * some index using a list of Words.
   *
   * @param words query of words
   * @param population words population
   * @return a new query result object.
   */
  static Result documents(List<Word> words, List<Word> population){
    Objects.requireNonNull(words);
    Objects.requireNonNull(population);

    final Index index = Index.createIndex(population);

    return Query.documents(words, index);
  }

  /**
   * @return a new query object.
   */
  static Query createQuery(){
    return new GeneralQuery();
  }

  /**
   * Searches the index for interesting documents.
   *
   * @param words list of words
   * @param index the corpus in a Matrix form.
   * @return a list of matching methods.
   */
  default Result documentSearch(List<Word> words, Index index){

    final List<Word> keywords       = Objects.requireNonNull(words);
    final Index      validIndex     = Objects.requireNonNull(index);
    final Matrix     queryMatrix    = createQueryVector(keywords, validIndex.wordList());
    final Matrix     tfidfMatrix    = Jamas.tfidfMatrix(index.wordDocFrequency());

    if(BasicExecutionMonitor.get().isActive()){

      BasicExecutionMonitor.get().info(
        String.format("Query#documentSearch: Printing query vector for %s.", keywords)
      );

      Jamas.printJamaMatrix(
        "Query vector (words)",
        queryMatrix,
        keywords
      );

      System.out.println();

      Jamas.printJamaMatrix(
        "Tf-idf matrix (words)",
        tfidfMatrix,
        validIndex.wordList()
      );

    }


    return documentSearch(queryMatrix, index.docSet(), tfidfMatrix);
  }

  /**
   * Searches the index for interesting words using a list of documents.
   *
   * @param documents list of documents
   * @param index the corpus in a Matrix form.
   * @return a list of matching words.
   */
  default Result wordSearch(List<Document> documents, Index index){

    final List<Document>  keydocs        = Objects.requireNonNull(documents);
    final Index           validIndex     = Objects.requireNonNull(index);
    final List<Document>  docList        = validIndex.docSet().stream().collect(Collectors.toList());
    final Matrix          queryMatrix    = createQueryVector(keydocs, docList);
    final Matrix          tfidfMatrix    = Jamas.tfidfMatrix(index.wordDocFrequency().transpose());

    if(BasicExecutionMonitor.get().isActive()){

      BasicExecutionMonitor.get().info(
        String.format("Query#wordSearch: Printing query vector for %s.", keydocs)
      );

      Jamas.printJamaMatrix(
        "Query vector (words)",
        queryMatrix,
        keydocs
      );

      System.out.println();

      Jamas.printJamaMatrix(
        "Tf-idf matrix (words)",
        tfidfMatrix.transpose(),
        validIndex.wordList()
      );
    }

    return wordSearch(queryMatrix, index.wordList(), tfidfMatrix);
  }

  /**
   * Searches for a list of documents that match a given query.
   *
   * @param query list of words as a query.
   * @param index the indexed corpus (as a matrix).
   * @return a list of matching methods.
   */
  default Result documentSearch(Matrix query, Set<Document> docSet, Matrix index) {
    final Map<Integer, Double> scores = new HashMap<>();

    for(Document each : docSet){
      double score = Jamas.computeSimilarity(query, Jamas.getCol(index, each.id()));
      if(Doubles.compare(score, 0.0D) > 0){
        scores.put(each.id(), score);
      }
    }


    final List<Document> docList = docSet.stream().collect(Collectors.toList());

    final List<Integer> indices = scores.entrySet().stream()
      .sorted((a, b) -> Double.compare(a.getValue(), b.getValue()))
      .map(Map.Entry::getKey).collect(Collectors.toList());

    Collections.reverse(indices);

    final List<Document> scoredDocList = indices.stream()
      .map(docList::get).collect(Collectors.toList());

    return Result.downcast(scoredDocList);
  }

  /**
   * Searches for a list of words that match a given query.
   *
   * @param query list of words as a query.
   * @param index the indexed corpus (as a matrix).
   * @return a list of matching methods.
   */
  default Result wordSearch(Matrix query, List<Word> wordList, Matrix index) {
    final Map<Integer, Double> scores = new LinkedHashMap<>();

    int idx = 0; for(Word ignored : wordList){
      double score = Jamas.computeSimilarity(query, Jamas.getCol(index, idx));
      if(Doubles.compare(score, 0.0D) > 0){
        scores.put(idx, score);
      } idx++;
    }


    final List<Word> docList = wordList.stream().collect(Collectors.toList());

    final List<Integer> indices = scores.entrySet().stream()
      .sorted((a, b) -> Double.compare(a.getValue(), b.getValue()))
      .map(Map.Entry::getKey).collect(Collectors.toList());

    Collections.reverse(indices);

    final List<Word> scoredDocList = indices.stream()
      .map(docList::get)
      .sorted((a, b) -> a.element().compareTo(b.element()))
      .collect(Collectors.toList());

    return Result.downcast(scoredDocList);
  }


  static <I> Matrix createQueryVector(List<I> keywords, List<I> terms){
    Matrix      queryMatrix = new Matrix(terms.size(), 1, 0.0D);

    for (I k : keywords) {
      int termIndex = 0;

      for (I w : terms) {
        if(Objects.equals(k, w)){
          queryMatrix.set(termIndex, 0, 1.0D);
        }

        termIndex++;
      }
    }

    queryMatrix = queryMatrix.times(1 / queryMatrix.norm1());

    return queryMatrix;
  }

  class Result implements Iterable<Object> {
    final List<Object> documents;

    Result(List<Object> documents){
      this.documents = documents;
    }

    static <I> Result downcast(List<I> items){
      return of(items.stream().map(s -> (Object) s).collect(Collectors.toList()));
    }

    static Result of(List<Object> items){
      return new Result(items);
    }


    /**
     * Automatically cast the items in the group to their correct type. It will fail fast
     * if trying to cast an item to an incorrect type.
     *
     * @param result result set returned by {@link Query}
     * @param klass target type
     * @param <I> item type
     * @return a new list of items; items were cast to their correct type.
     * @throws ClassCastException if trying to cast an object to a subclass of which it is
     *  not an instance.
     */
    public static <I> List<I> items(Result result, Class<I> klass){
      return result.documents.stream()
        .map(klass::cast).collect(Collectors.toList());
    }

    @Override public Iterator<Object> iterator() {
      return documents.iterator();
    }

    @Override public String toString() {
      return documents.toString();
    }
  }

  class GeneralQuery implements Query {}

}
