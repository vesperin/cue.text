package com.vesperin.text.utils;

import java.util.List;
import java.util.Objects;

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
}
