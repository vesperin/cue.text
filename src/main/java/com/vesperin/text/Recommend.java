package com.vesperin.text;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.utils.Strings;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toConcurrentMap;

/**
 * @author Huascar Sanchez
 */
public interface Recommend {

  /**
   * Coalesces a list of labels into one label.
   *
   * @param labels list of labels to be coalesced.
   * @return coalesced label.
   */
  static String coalesce(List<String> labels){
    Objects.requireNonNull(labels);

    return Joiner.on(";").join(Lists.newArrayList(labels.stream()
      .collect(Collectors.toCollection(LinkedList::new))
      .descendingIterator()));
  }

  /**
   * @return a new recommend object.
   */
  static Recommend createRecommend(){
    return new LabelRecommend();
  }


  /**
   * Search for frequently occurring labels in a list of documents with similar names.
   *
   * @param documents current list of documents
   * @return a new query result object.
   */
  static List<String> labels(List<Document> documents){
    Objects.requireNonNull(documents);
    return createRecommend().recommendLabels(documents);
  }

  /**
   * Extracts most frequent labels in the list of documents. This method
   * extracts labels from the qualifiedClassname.
   *
   * @param documents list of documents
   * @return a list of matching labels.
   */
  default List<String> recommendLabels(List<Document> documents) {
    // we don't accept plurals and stop words
    final List<String> allStrings = documents.stream()
      .flatMap(s -> Arrays.stream(Strings.wordSplit(s.transformedName())))
      .collect(Collectors.toList());

    // frequency calculation
    final Map<String, Integer> scores = allStrings.stream()
      .filter(s -> (s.length() > 2))
      .collect(toConcurrentMap(w -> w, w -> 1, Integer::sum));

    // sort entries in ascending order
    List<Map.Entry<String, Integer>> firstPass = scores.entrySet().stream()
      .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
      .filter(e -> (documents.size() <= 1 || e.getValue() > 1))
      .collect(Collectors.toList());

    final int k = documents.size() == 1 ? scores.size() : (int) Math.floor(Math.sqrt(scores.size()));

    List<String> secondPass = firstPass.stream()
      .map(Map.Entry::getKey)
      .map(String::toLowerCase)
      .limit(k).collect(Collectors.toList());

    secondPass = secondPass.isEmpty()
      ? allStrings.stream().distinct().filter(s -> s.length() > 1).map(String::toLowerCase).collect(Collectors.toList())
      : secondPass;

    return secondPass;
  }

  class LabelRecommend implements Recommend {}
}
