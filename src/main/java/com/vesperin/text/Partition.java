package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.text.Selection.Word;

import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Partition<T> {
  final String          label;
  final Set<Word>       words;
  final Set<Project<T>> projects;

  private Partition(String name){
    this.label    = name;
    this.words    = Sets.newHashSet();
    this.projects = Sets.newHashSet();
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

  @Override public String toString() {
    return this.label;
  }
}
