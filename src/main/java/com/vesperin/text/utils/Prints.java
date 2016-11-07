package com.vesperin.text.utils;

import com.vesperin.text.Selection.Word;

import java.util.*;

/**
 * @author Huascar Sanchez
 */
public class Prints {
  private Prints(){}

  public static <W> List<W> toPrettyPrintedList(List<W> wordList, boolean counts){
    return new PrettyPrintingList<>(wordList, counts);
  }

  public static <K, V> Map <K, V> toPrettyPrintedMap(Map<K, V> map){
    return new PrettyPrintingMap<>(map);
  }

  private static class PrettyPrintingList<W> extends ArrayList<W>{
    private final List<W> list;
    private final boolean counts;

    PrettyPrintingList(List<W> list, boolean counts){
      this.list = list;
      this.counts = counts;
    }

    @Override public boolean add(W w) {
      throw new UnsupportedOperationException("Read only");
    }

    @Override public void add(int index, W element) {
      throw new UnsupportedOperationException("Read only");
    }

    @Override public boolean addAll(Collection<? extends W> c) {
      throw new UnsupportedOperationException("Read only");
    }

    @Override public boolean addAll(int index, Collection<? extends W> c) {
      throw new UnsupportedOperationException("Read only");
    }

    @Override public String toString() {
      final StringBuilder sb = new StringBuilder("{\n");
      final Iterator<W> iterator = list.iterator();

      while(iterator.hasNext()){
        final W word = iterator.next();
        final boolean isWord = (word instanceof Word) && counts;

        sb.append("\t");
        sb.append(word);
        if(isWord){
          sb.append("=");
          sb.append('"');
          sb.append(((Word)word).value());
          sb.append('"');
        }

        if(iterator.hasNext()){
          sb.append(',').append(' ').append("\n");
        }
      }

      sb.append("\n}");

      return sb.toString();
    }
  }

  // thx to http://stackoverflow.com/questions/10120273/pretty-print-a-map-in-java
  private static class PrettyPrintingMap<K, V> extends HashMap <K, V> {
    private final Map<K, V> map;

    PrettyPrintingMap(Map<K, V> map) {
      this.map = map;
    }

    @Override public V put(K key, V value) {
      throw new UnsupportedOperationException("Read only");
    }

    @Override public void putAll(Map<? extends K, ? extends V> m) {
      throw new UnsupportedOperationException("Read only");
    }

    @Override public String toString() {
      final StringBuilder sb = new StringBuilder("{\n");
      final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();

      while (iterator.hasNext()) {
        final Map.Entry<K, V> entry = iterator.next();
        sb.append("\t");
        sb.append(entry.getKey());
        sb.append('=').append('"');
        sb.append(entry.getValue());
        sb.append('"');

        if (iterator.hasNext()) {
          sb.append(',').append(' ').append("\n");
        }
      }

      sb.append("\n}");
      return sb.toString();

    }
  }

  // thx to https://www.mkyong.com/java/how-to-sort-a-map-in-java/
  public static Map<String, Double> sortByValue(Map<String, Double> unsortedMap) {

    // 1. Convert Map to List of Map
    final List<Map.Entry<String, Double>> list = new LinkedList<>(unsortedMap.entrySet());

    // 2. Sort list with Collections.sort(), provide a custom Comparator
    //    Try switch the o1 o2 position for a different order
    Collections.sort(list, (o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

    // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
    final Map<String, Double> sortedMap = new LinkedHashMap<>();
    for (Map.Entry<String, Double> entry : list) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }

    return sortedMap;
  }
}
