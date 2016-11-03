package com.vesperin.text.groups.kmeans;

import Jama.Matrix;
import com.vesperin.text.Grouping;
import com.vesperin.text.Index;
import com.vesperin.text.Selection;
import com.vesperin.text.utils.Jamas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
public class WordKMeans extends KmeansMagnet {

  @Override public Grouping.Groups apply(List<Selection.Word> words) {
    final Index index = Index.createIndex(words);

    final Map<Selection.Word, Matrix> wordToMatrix = Jamas.splitMatrix(index.wordList(), index.wordDocFrequency());

    // prelim work
    int numDocs = words.size();
    int numGroups = (int) Math.floor(Math.sqrt(numDocs));

    final List<Selection.Word> initialClusters = new ArrayList<>(numGroups);
    initialClusters.addAll(
      words.stream().limit(numGroups).collect(toList())
    );

    // build initial clusters
    final List<Grouping.VectorGroup> clusters = new ArrayList<>();
    for (int i = 0; i < numGroups; i++) {
      final Grouping.VectorGroup cluster = Grouping.newVectorGroup();
      cluster.add(initialClusters.get(i), wordToMatrix.get(words.get(i)));
      clusters.add(cluster);
    }

    final List<Grouping.VectorGroup> prevClusters = new ArrayList<>();

    while (true) {
      int i;
      for (i = 0; i < numGroups; i++) {
        clusters.get(i).computeCenter();
      }

      for (i = 0; i < numDocs; i++) {
        int bestCluster = 0;
        double maxDistance = Double.MIN_VALUE;
        final Selection.Word word = words.get(i);
        final Matrix doc = wordToMatrix.get(word);

        for (int j = 0; j < numGroups; j++) {
          final double distance = clusters.get(j).proximity(doc);
          if (distance > maxDistance) {
            bestCluster = j;
            maxDistance = distance;
          }
        }

        clusters.stream()
          .filter(cluster -> cluster.vector(word) != null)
          .forEach(cluster -> cluster.remove(word));

        clusters.get(bestCluster).add(word, doc);
      }

      if (equals(clusters, prevClusters)) {
        break;
      }

      prevClusters.clear();
      prevClusters.addAll(clusters);
    }

    return Grouping.Groups.of(clusters, index);
  }

}