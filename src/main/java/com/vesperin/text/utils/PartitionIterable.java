package com.vesperin.text.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class implements an {@code Iterable} over all partitions of a given
 * list.
 *
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Feb 14, 2016 a.k.a. Friend Edition)
 * @param <T> The actual element type.
 */
public class PartitionIterable<T> implements Iterable<List<List<T>>> {

  private final List<T> allElements = new ArrayList<>();
  private final int blocks;

  public PartitionIterable(List<T> allElements, int blocks) {
    checkNumberOfBlocks(blocks, allElements.size());
    this.allElements.addAll(allElements);
    this.blocks = blocks;
  }

  @Override public Iterator<List<List<T>>> iterator() {
    return new PartitionIterator<>(allElements, blocks);
  }

  private void checkNumberOfBlocks(int blocks, int numberOfElements) {
    if (blocks < 1) {
      throw new IllegalArgumentException(
        "The number of blocks should be at least 1, received: " +
          blocks);
    }

    if (blocks > numberOfElements) {
      throw new IllegalArgumentException(
        "The number of blocks should be at most " +
          numberOfElements + ", received: " + blocks);
    }
  }

  private static final class PartitionIterator<T>
    implements Iterator<List<List<T>>> {

    private List<List<T>> nextPartition;
    private final List<T> allElements = new ArrayList<>();
    private final int blocks;

    private final int[] s;
    private final int[] m;
    private final int n;

    PartitionIterator(List<T> allElements, int blocks) {
      this.allElements.addAll(allElements);
      this.blocks = blocks;
      this.n = allElements.size();

      s = new int[n];
      m = new int[n];

      if (n != 0) {
        for (int i = 0; i < n - blocks + 1; ++i) {
          s[i] = 0;
          m[i] = 0;
        }

        for (int i = n - blocks + 1; i < n; ++i) {
          s[i] = m[i] = i - n + blocks;
        }

        loadPartition();
      }
    }

    @Override public boolean hasNext() {
      return nextPartition != null;
    }

    @Override public List<List<T>> next() {
      if (nextPartition == null) {
        throw new NoSuchElementException("No more partitions left.");
      }

      List<List<T>> partition = nextPartition;
      generateNextPartition();
      return partition;
    }

    private void loadPartition() {
      nextPartition = new ArrayList<>(blocks);

      for (int i = 0; i < blocks; ++i) {
        nextPartition.add(new ArrayList<>());
      }

      for (int i = 0; i < n; ++i) {
        nextPartition.get(s[i]).add(allElements.get(i));
      }
    }

    private void generateNextPartition() {
      for (int i = n - 1; i > 0; --i) {
        if (s[i] < blocks - 1 && s[i] <= m[i - 1]) {
          s[i]++;
          m[i] = Math.max(m[i], s[i]);

          for (int j = i + 1; j < n - blocks + m[i] + 1; ++j) {
            s[j] = 0;
            m[j] = m[i];
          }

          for (int j = n - blocks + m[i] + 1; j < n; ++j) {
            s[j] = m[j] = blocks - n + j;
          }

          loadPartition();
          return;
        }
      }

      nextPartition = null;
    }
  }

  public static void main(String[] args) {
    List<Object> list = Arrays.asList("A", "B", "C", "D", "E");
    int row = 1;

    for (int blocks = 1; blocks <= list.size(); ++blocks) {
      for (List<List<Object>> partition :
        new PartitionIterable<>(list, blocks)) {
        System.out.printf("%2d: %s\n", row++, partition);
      }
    }
  }
}