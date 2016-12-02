package com.vesperin.text.utils;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import com.google.common.collect.Iterables;
import com.vesperin.text.Selection.Word;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Huascar Sanchez
 */
public class Jamas {
  private Jamas(){}

  /**
   * Gets the specified row of a matrix.
   *
   * @param m the matrix.
   * @param row the row to get.
   * @return the specified row of m.
   */
  public static Matrix getRow(Matrix m, int row) {
    return m.getMatrix(row, row, 0, m.getColumnDimension() - 1);
  }

  /**
   * Deletes a column from a matrix.  Does not change the passed matrix.
   * @param m the matrix.
   * @param col the column to delete.
   * @return m with the specified column deleted.
   */
  public static Matrix deleteCol(Matrix m, int col) {
    int numRows = m.getRowDimension();
    int numCols = m.getColumnDimension();
    Matrix m2 = new Matrix(numRows,numCols-1);
    for (int mj=0,m2j=0; mj < numCols; mj++) {
      if (mj == col)
        continue;  // skips incrementing m2j
      for (int i=0; i<numRows; i++) {
        m2.set(i,m2j,m.get(i,mj));
      }

      m2j++;
    }

    return m2;
  }

  /**
   * Computes cosine similarity between two matrices.
   *
   * @param sourceDoc matrix a
   * @param targetDoc matrix b
   * @return cosine similarity score
   */
  public static double computeSimilarity(Matrix sourceDoc, Matrix targetDoc) {
    double dotProduct     = sourceDoc.arrayTimes(targetDoc).norm1();
    double euclideanDist  = sourceDoc.normF() * targetDoc.normF();
    return dotProduct / euclideanDist;
  }

  /**
   *
   * @param matrix
   * @return
   */
  public static Matrix buildSimilarityMatrix(Matrix matrix){
    final int numDocs = matrix.getColumnDimension();
    final Matrix similarityMatrix = new Matrix(numDocs, numDocs);
    for (int i = 0; i < numDocs; i++) {
      final Matrix sourceDocMatrix = Jamas.getCol(matrix, i);
      for (int j = 0; j < numDocs; j++) {
        final Matrix targetDocMatrix = Jamas.getCol(matrix, j);
        similarityMatrix.set(i, j,
          Jamas.computeSimilarity(sourceDocMatrix, targetDocMatrix));
      }
    }

    return similarityMatrix;
  }

  public static void printRawFreqMatrix(Matrix matrix, List<String> documentNames, List<Word> words){
    printMatrix("Raw Frequency Matrix", matrix, documentNames, words, new PrintWriter(System.out));
  }


  public static <T> void printJamaMatrix(String legend, Matrix matrix, List<T> items){
    System.out.printf("=== %s ===%n", legend);

    final double[][] m = matrix.getArray();

    for (int i = 0; i < m[0].length; i++) {
      if(i == 0){
        System.out.printf("%20s", "D" + i);
      } else {
        System.out.printf("%8s", "D" + i);
      }
    }

    System.out.println();
    for (int i = 0; i < m.length; i++) {
      if(!containsItemAt(items, i)) continue;
      System.out.printf("%15s", items.get(i));
      for (int j = 0; j < m[0].length; j++) {
        double val = m[i][j];
        val = Double.isNaN(val) ? 0.0D : val;
        System.out.printf("%8.4f", val);
      }

      System.out.println();
    }

    System.out.println();
    System.out.println();

  }

  private static <T> boolean containsItemAt(List<T> items, int idx){
    try { items.get(idx); return true; } catch (Exception e) {
      return false;
    }
  }


  public static void printMatrix(String legend, Matrix matrix, List<String> documentNames, List<Word> words, PrintWriter writer) {
    writer.printf("=== %s ===%n", legend);
    writer.printf("%15s", " ");
    for (int i = 0; i < documentNames.size(); i++) {
      writer.printf("%8s", "D" + i);
    }

    writer.println();
    for (int i = 0; i < words.size(); i++) {
      writer.printf("%15s", words.get(i).element());
      for (int j = 0; j < documentNames.size(); j++) {
        double val = matrix.get(i, j);
        val = Double.isNaN(val) ? 0.0D : val;
        writer.printf("%8.4f", val);
      }

      writer.println();
    }

    writer.println();
    writer.println();

    for (int i = 0; i < documentNames.size(); i++) {
      writer.println(i + " - " + documentNames.get(i));
    }

    writer.flush();
  }

  public static Matrix tfidfMatrix(Matrix matrix){

    // Phase 1: apply IDF weight to the raw word frequencies
    int n = matrix.getColumnDimension();
    for (int j = 0; j < matrix.getColumnDimension(); j++) {
      for (int i = 0; i < matrix.getRowDimension(); i++) {
        double matrixElement = matrix.get(i, j);

        if (matrixElement > 0.0D) {

          final Matrix subMatrix  = matrix.getMatrix(i, i, 0, matrix.getColumnDimension() - 1);
          final double dm         = countDocsWithWord(subMatrix);
          final double tfIdf      = matrix.get(i,j) * (1 + Math.log(n) - Math.log(dm));

          matrix.set(i, j, tfIdf);
        }
      }
    }

    // Phase 2: normalize the word scores for a single document
    for (int j = 0; j < matrix.getColumnDimension(); j++) {
      final Matrix colMatrix  = getCol(matrix, j);
      final double sum        = colSum(colMatrix);

      for (int i = 0; i < matrix.getRowDimension(); i++) {
        matrix.set(i, j, (matrix.get(i, j) / sum));
      }
    }

    return matrix;
  }


  private static double countDocsWithWord(Matrix rowMatrix) {
    double numDocs = 0.0D;
    for (int j = 0; j < rowMatrix.getColumnDimension(); j++) {
      if (rowMatrix.get(0, j) > 0.0D) {
        numDocs++;
      }
    }

    return numDocs;
  }

  public static Matrix createQueryVector(List<Word> query, Matrix corpus/*raw frequencies*/){

    // phase 1: Singular value decomposition
    final SingularValueDecomposition svd = new SingularValueDecomposition(corpus);
    final Matrix wordVector     = svd.getU();
    final Matrix sigma          = getSigma(corpus.getRowDimension(), svd.getSingularValues());

    // compute the value of k (i.e., # latent dimensions)
    final int k = (int) Math.floor(Math.sqrt(corpus.getColumnDimension()));
    double[][] queryVector = new double[k][1];


//    Arrays.fill(queryVector, 0.0D);

    // populate query vector with real values
    for(int i = 0; i < query.size(); i++){
      for(int j = 0; j < k; j++){
        final double val = corpus.get(i, j);
        queryVector[j][0] += Double.isNaN(val) ? 0.0D : val;
      }
    }

    final Matrix reducedWordVector = wordVector.getMatrix(
      0, wordVector.getRowDimension() - 1, 0, k - 1);

    final Matrix reducedSigma = sigma.getMatrix(0, k - 1, 0, k - 1);

    final Matrix queryMatrix = new Matrix(queryVector);

    return queryMatrix.transpose()
      .times(reducedWordVector)
      .times(reducedSigma.inverse());
  }

  public static Matrix latentSemanticIndexing(Matrix matrix){
    // compute the value of k (ie where to truncate) // used to be getColDimensions
    int k = (int) Math.floor(Math.sqrt(matrix.getRowDimension()));

    // phase 1: Get singular value decomposition
    final SingularValueDecomposition svd = matrix.svd();

    final Matrix U  = svd.getU();
    final Matrix S  = getSigma(matrix.getRowDimension(), svd.getSingularValues());//svd.getS();
    final Matrix V  = svd.getV();

    final Matrix reducedU  = U.getMatrix(0, U.getRowDimension() - 1, 0, k - 1);
    final Matrix reducedS  = S.getMatrix(0, k - 1, 0, k - 1);
    final Matrix reducedV  = V.getMatrix(0, V.getRowDimension() - 1, 0, k - 1);

    final Matrix weights = reducedU.times(reducedS)
      .times(reducedV.transpose());

    // Phase 2: Normalize the word score for a single document
    for (int j = 0; j < weights.getColumnDimension(); j++) {
      double sum = colSum(getCol(weights, j));

      for (int i = 0; i < weights.getRowDimension(); i++) {
        weights.set(i, j, Math.abs((weights.get(i, j)) / sum));
      }
    }

    return weights;
  }

  private static Matrix getSigma(int cols, double[] singularValues){
    Matrix var1 = new Matrix(cols, cols);
    double[][] var2 = var1.getArray();

    for(int var3 = 0; var3 < cols; ++var3) {
      for(int var4 = 0; var4 < cols; ++var4) {
        var2[var3][var4] = 0.0D;
      }

      var2[var3][var3] = (var3 > singularValues.length - 1
        ? 0.00D
        : singularValues[var3]
      );
    }

    return var1;
  }


  public static double colSum(Matrix colMatrix) {
    double sum = 0.0D;
    for (int i = 0; i < colMatrix.getRowDimension(); i++) {
      sum += colMatrix.get(i, 0);
    }

    if(Double.isNaN(sum)){
      System.out.println("Invalid sum: " + sum);
    }

    return sum;
  }

  /**
   * Gets the specified column of a matrix.
   *
   * @param m the matrix.
   * @param col the column to get.
   * @return the specified column of m.
   */
  public static Matrix getCol(Matrix m, int col) {
    return m.getMatrix(0, m.getRowDimension() - 1, col, col);
  }

  /**
   * Gets the sum of the specified row of the matrix.
   *
   * @param m the matrix.
   * @param row the row.
   * @return the sum of m[row,*]
   */
  public static double rowSum(Matrix m, int row) {
    // error check the column index
    if (row < 0 || row >= m.getRowDimension()) {
      throw new IllegalArgumentException(
        "row exceeds the row indices [0,"+(m.getRowDimension() - 1)+"] for m."
      );
    }

    double rowSum = 0;

    // loop through the rows for this column and compute the sum
    int numCols = m.getColumnDimension();
    for (int j = 0; j < numCols; j++) {
      final double val = m.get(row,j);
      rowSum += Double.isNaN(val) ? 0.0D : val;
    }

    return rowSum;
  }

  /** Multiply a matrix by a scalar, C = s*A
   @param s    scalar
   @return     s*A
   */

  public static Matrix div(Matrix matrix, double s) {
    final int m = matrix.getRowDimension();
    final int n = matrix.getColumnDimension();
    final Matrix X = new Matrix(m, n);

    final double[][] C = X.getArray();
    final double[][] A = matrix.getArray();

    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        C[i][j] = A[i][j]/s;
      }
    }
    return X;
  }

  public static <T> Map<T, Matrix> splitMatrix(List<T> items, Matrix matrix){
    final Map<T, Matrix> map = new HashMap<>();
    int idx = 0; for (T each : items){
      map.put(each, Jamas.getRow(matrix, idx));
      idx++;
    }

    return map;
  }
}

