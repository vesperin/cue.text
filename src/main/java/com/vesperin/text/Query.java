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
   * @param words keywords query
   * @param index existing corpus index
   * @return a new query result object.
   */
  static Result methods(List<Word> words, Index index){
    return createQuery().search(words, index);
  }

  /**
   * @return a new query object.
   */
  static Query createQuery(){
    return new MethodQuery();
  }

  /**
   * Searches the index for interesting documents.
   *
   * @param words list of words
   * @param index the corpus in a Matrix form.
   * @return a list of matching methods.
   */
  default Result search(List<Word> words, Index index){

    final List<Word> keywords       = Objects.requireNonNull(words);
    final Index      validIndex     = Objects.requireNonNull(index);
    final Matrix     queryMatrix    = createQueryVector(keywords, validIndex);

    return search(queryMatrix, index.docSet(), index.lsiMatrix());
  }

  /**
   * Searches for a list of methods that match a given query.
   *
   * @param query list of words as a query.
   * @param index the indexed corpus (as a matrix).
   * @return a list of matching methods.
   */
  default Result search(Matrix query, Set<Document> docSet, Matrix index) {
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

    return Result.of(scoredDocList);
  }

  static Matrix createQueryVector(List<Word> keywords, Index index){
    final List<Word>  terms       = index.wordList();

    Matrix      queryMatrix = new Matrix(terms.size(), 1, 0.0D);

    for (Word k : keywords) {
      int termIndex = 0;

      for (Word w : terms) {
        if(Objects.equals(k, w)){
          queryMatrix.set(termIndex, 0, 1.0D);
        }

        termIndex++;
      }
    }

    queryMatrix = queryMatrix.times(1 / queryMatrix.norm1());

    return queryMatrix;
  }

  class Result implements Iterable<Document> {
    final List<Document> documents;

    Result(List<Document> documents){
      this.documents = documents;
    }

    static Result of(List<Document> documents){
      return new Result(documents);
    }

    @Override public Iterator<Document> iterator() {
      return documents.iterator();
    }

    @Override public String toString() {
      return documents.toString();
    }
  }

  class MethodQuery implements Query {}

}
