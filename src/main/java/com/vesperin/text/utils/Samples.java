package com.vesperin.text.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Samples {
  private Samples(){}

  /**
   * Choose the K value from a list of objects of type {@literal T}.
   * @param data list of objects of type {@literal T}.
   * @param <T> type of objects in data list
   * @return the k value
   */
  public static <T> int chooseK(List<T> data){
    if(Objects.isNull(data)) return 0;
    if(data.isEmpty())       return 0;

    return (int) Math.ceil(Math.sqrt(data.size()));
  }

  /**
   * Gets a set of common elements from a list of sets.
   *
   * @param sortedList sorted list of sets (by size).
   * @param <T> type of elements in the sets.
   * @return a new set
   */
  public static <T> Set<T> getCommonElements(List<? extends Set<T>> sortedList) {

    final Set<T> common = new LinkedHashSet<>();

    if (!sortedList.isEmpty()) {

      for(int idx = 0; idx < sortedList.size(); idx++){
        if(idx == 0) {
          common.addAll(sortedList.get(idx));
        } else {
          common.retainAll(sortedList.get(idx));

          if(common.isEmpty()){
            common.addAll(sortedList.get(idx));
          }
        }
      }
    }

    return common;
  }

  /**
   * Gets a set of unique elements from a list of sets.
   *
   * @param collections set of sets (by size).
   * @param <T> type of elements in the sets.
   * @return a new set
   */
  public static <T> Set<Set<T>> getUniqueElements(Set<? extends Set<T>> collections) {

    List<Set<T>> allUniqueSets = new ArrayList<>();
    for (Collection<T> collection : collections) {
      Set<T> unique = new LinkedHashSet<>(collection);
      allUniqueSets.add(unique);
      collections.stream()
        .filter(otherCollection -> !Objects.equals(collection, otherCollection))
        .forEach(unique::removeAll);
    }

    return allUniqueSets.stream().collect(Collectors.toSet());
  }
}
