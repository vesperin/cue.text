package com.vesperin.text;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import com.google.common.collect.Lists;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.utils.Jamas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Index {
  private int docCount;
  private int wordCount;

  private Map<Document, List<Word>> indexMap;
  private Map<String, Document>     docMap;

  private Matrix      wordFrequencyMatrix;
  private Matrix      lsiMatrix;
  private List<Word>  wordList;
  private Set<String> docSet;

  Index(){
    this.wordCount  = 0;
    this.docCount   = 0;
    this.indexMap   = new ConcurrentHashMap<>();
    this.docMap     = new ConcurrentHashMap<>();
    this.wordList   = new CopyOnWriteArrayList<>();
    this.docSet     = new HashSet<>();

    this.wordFrequencyMatrix  = null;
    this.lsiMatrix            = null;
  }

  /**
   * It creates an index based on a flatten word list
   * (Set this ({@link Selection#flattenWordList(Set)} method first).
   *
   * @param words flatten Word List.
   * @return a new Index object.
   */
  static Index createIndex(List<Word> words){
    final Index index = new Index();
    index.index(words);
    index.createWordDocMatrix();
    index.createLsiMatrix();
    return index;
  }

  Set<Document> docSet(){
    return indexMap.keySet();
  }

  List<Word> wordList(){
    return wordList;
  }

  Matrix wordDocFrequency(){
    return wordFrequencyMatrix;
  }

  Matrix lsiMatrix(){
    return lsiMatrix;
  }

  void index(List<Word> words/*unique*/){

    final Map<String, List<Word>> map = new HashMap<>();
    final Set<Word> wordsSet = new LinkedHashSet<>();
    wordsSet.addAll(words);

    for(Word each : words){
      final Set<String> docs = each.container();

      for(String doc : docs){
        if(!map.containsKey(doc)){
          map.put(doc, Lists.newArrayList(each));
        } else {
          map.get(doc).add(each);
        }
      }
    }

    int idx = 0; for(String each : map.keySet()){
      final Document doc = new Selection.DocumentImpl(idx, each);
      indexMap.put(doc, map.get(each));
      docMap.put(each, doc);
      docSet.add(each);
      idx++;
    }

    docCount  = indexMap.keySet().size();
    wordCount = wordsSet.size();
    wordList.addAll(wordsSet);
  }

  void createWordDocMatrix(){
    final double[][] data = new double[wordCount][docCount];

    final List<String> docList = docSet.stream()
      .collect(Collectors.toList());

    for (int i = 0; i < wordCount; i++) {
      for (int j = 0; j < docCount; j++) {

        final List<Word> ws = indexMap.get(docMap.get(docList.get(j)));
        if(ws == null) continue;

        final Word word = wordList.get(i);

        int count = 0; for(Word each : ws){
          if(Objects.equals(each, word)) count++;
        }

        data[i][j] = count;
      }
    }

    wordFrequencyMatrix = new Matrix(data);
  }

  Matrix createLsiMatrix(){
    final Matrix matrix = wordDocFrequency().transpose();
    // compute the value of k (ie where to truncate)
    int k = (int) Math.floor(Math.sqrt(matrix.getColumnDimension()));

    final SingularValueDecomposition svd = matrix.svd();

    final Matrix U  = svd.getU();
    final Matrix S  = svd.getS();
    final Matrix V  = svd.getV();

    final Matrix reducedU  = U.getMatrix(0, U.getRowDimension() - 1, 0, k - 1);
    final Matrix reducedS  = S.getMatrix(0, k - 1, 0, k - 1);
    final Matrix reducedV  = V.getMatrix(0, V.getRowDimension() - 1, 0, k - 1);

    final Matrix weights = reducedU.times(reducedS)
      .times(reducedV.transpose());

    // Phase 2: Normalize the word score for a single document
    for (int j = 0; j < weights.getColumnDimension(); j++) {
      double sum = Jamas.colSum(Jamas.getCol(weights, j));

      for (int i = 0; i < weights.getRowDimension(); i++) {
        weights.set(i, j, Math.abs((weights.get(i, j)) / sum));
      }
    }

    lsiMatrix = weights.transpose();
    return lsiMatrix;
  }


}
