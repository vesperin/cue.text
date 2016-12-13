package com.vesperin.text;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.vesperin.text.Selection.Word;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Partition<T> {
  private final StringBuilder   label;
  private final Set<Word>       words;
  private final Set<Project<T>> projects;

  private Partition(String name){
    this.label    = new StringBuilder(name);
    this.words    = Sets.newHashSet();
    this.projects = Sets.newHashSet();
  }

  /**
   * Creates a new Partition object.
   *
   * @param <T> the type of the object enclosed in the partition.
   * @return a new Partition object.
   */
  public static <T> Partition<T> newPartition(){
    return newPartition("");
  }

  /**
   * Creates a new Partition object.
   *
   * @param label the name of this partition.
   * @param <T> the type of the object enclosed in the partition.
   * @return a new Partition object.
   */
  public static <T> Partition<T> newPartition(String label){
    Objects.requireNonNull(label);
    return new Partition<>(label);
  }

  /**
   * Adds a project to the set of current projects.
   * @param project new project
   */
  public void add(Project<T> project){
    this.projects.add(project);
  }

  /**
   * Adds a set of projects to the current set of projects in this
   * partition object iff they share a few words.
   *
   * @param projectSet the set of projects
   */
  public void addAllIff(Set<Project<T>> projectSet, Set<Word> shared){
    projectSet.stream()
      .filter(each -> contains(shared, each.wordSet()))
      .forEach(this::add);

    updateName();
  }

  private void updateName(){
    label.setLength(0);

    final Iterator<Project<T>> iterator  = projectSet().iterator();
    final StringBuilder name = new StringBuilder(1000);

    while(iterator.hasNext()){
      final Project<T> each = iterator.next();

      name.append(each.name());
      if(iterator.hasNext()){
        name.append(" ∩ ");
      }

    }

    label.append("(").append(name).append(")");
  }

  private static boolean contains(Set<Word> src, Set<Word> dst){
//    final int overlap = Samples.chooseK(dst.stream().collect(Collectors.toList()));

    return dst.containsAll(src);
    //return Sets.intersection(src, dst).size() >= 1;

//    for(Word each : dst){
//      if(src.contains(each)) return true;
//    }
//
//    return false;
  }

  public static <T> Set<Word> getCommonElements(Set<Project<T>> projects){
    final List<Set<Word>> all = projects.stream()
      .map(Project::wordSet)
      .sorted((a, b) -> Ints.compare(a.size(), b.size()))
      .collect(Collectors.toList());

    return getCommonElements(all);
  }

  private static <T> Set<T> getCommonElements(List<? extends Set<T>> sortedList) {

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
   * Adds a set of projects to the current set of projects in this
   * partition object.
   *
   * @param projectSet a new set of projects.
   */
  public void addAll(Set<Project<T>> projectSet){
    projectSet.forEach(this::add);
  }

  /**
   * Copies another set of words to this current set.
   * @param otherWords the other set
   */
  public void copiesAll(Set<Word> otherWords){
    this.words.addAll(otherWords);
  }


  /**
   * @return the partition's label
   */
  public String label(){
    return label.toString();
  }

  /**
   * @return the set of words
   */
  public Set<Word> wordSet(){
    return words;
  }

  /**
   * @return the current project set.
   */
  public Set<Project<T>> projectSet(){
    return projects;
  }


  @Override public boolean equals(Object obj) {
    if (null == obj)
      return false;

    if (obj instanceof Partition) {
      if (((Partition<?>) obj).label().equals(this.label()))
        return true;
    }

    return false;
  }

  @Override public int hashCode() {
    return Objects.hash(label());
  }

  @Override public String toString() {
    return this.label();
  }
}
