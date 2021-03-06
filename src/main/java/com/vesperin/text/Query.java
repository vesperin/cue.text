package com.vesperin.text;

import Jama.Matrix;
import com.google.common.primitives.Doubles;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.utils.Jamas;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
   * Search for interesting methods in some index using a list of keywords.
   *
   * @param words query of keywords
   * @param index existing index
   * @return a new query result object.
   */
  static Result methods(List<Word> words, Index index){
    return createQuery().methodSearch(words, index);
  }

  /**
   * Search for interesting fully.qualified.ClassName#methodName entries in
   * some index using a list of Documents.
   *
   * @param docs query of documents
   * @param index existing index
   * @return a new query result object.
   */
  static Result types(List<Document> docs, Index index){
    return createQuery().typeSearch(docs, index);
  }

  /**
   * @return a new query object.
   */
  static Query createQuery(){
    return new MethodQuery();
  }

  /**
   * Searches the index for interesting methods.
   *
   * @param words list of words
   * @param index the corpus in a Matrix form.
   * @return a list of matching methods.
   */
  default Result methodSearch(List<Word> words, Index index){

    final List<Word> keywords       = Objects.requireNonNull(words);
    final Index      validIndex     = Objects.requireNonNull(index);
    final Matrix     queryMatrix    = createQueryVector(keywords, validIndex.wordList());

    return methodSearch(queryMatrix, index.docSet(), index.lsiMatrix());
  }

  /**
   * Searches the index for interesting types.
   *
   * @param documents list of documents
   * @param index the corpus in a Matrix form.
   * @return a list of matching types.
   */
  default Result typeSearch(List<Document> documents, Index index){

    final List<Document>  keydocs        = Objects.requireNonNull(documents);
    final Index           validIndex     = Objects.requireNonNull(index);
    final List<Document>  docList        = validIndex.docSet().stream().collect(Collectors.toList());
    final Matrix          queryMatrix    = createQueryVector(keydocs, docList);

    return typeSearch(queryMatrix, index.wordList(), index.lsiMatrix().transpose());
  }

  /**
   * Searches for a list of methods that match a given query.
   *
   * @param query list of words as a query.
   * @param index the indexed corpus (as a matrix).
   * @return a list of matching methods.
   */
  default Result methodSearch(Matrix query, Set<Document> docSet, Matrix index) {
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
   * Searches for a list of methods that match a given query.
   *
   * @param query list of words as a query.
   * @param index the indexed corpus (as a matrix).
   * @return a list of matching methods.
   */
  default Result typeSearch(Matrix query, List<Word> wordList, Matrix index) {
    final Map<Integer, Double> scores = new HashMap<>();

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
      .map(docList::get).collect(Collectors.toList());

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
    static <I> List<I> items(Result result, Class<I> klass){
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

  class MethodQuery implements Query {}

}
