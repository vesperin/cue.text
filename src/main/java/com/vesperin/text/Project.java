package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.tokenizers.WordsTokenizer;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Project <T> {
  private final String    name;
  private final Corpus<T> corpus;
  private final Set<Word> words;

  private Project(String name, Corpus<T> corpus, WordsTokenizer tokenizer){
    this.name   = name;
    this.corpus = corpus;
    this.words  = Introspector.typicalityRank(corpus, tokenizer)
      .stream().collect(Collectors.toSet());
  }

  private Project(String name, Set<Word> words){
    this.name   = name;
    this.words  = words;
    this.corpus = Corpus.ofGenericType(Sets.newHashSet());
  }

  /**
   * Creates a new project.
   *
   * @param name the name of a project
   * @param corpus  corpus object
   * @param tokenizer word tokenizer
   * @param <T> element type
   * @return new project object
   */
  public static <T> Project<T> createProject(String name, Corpus<T> corpus, WordsTokenizer tokenizer){
    return new Project<>(name, corpus, tokenizer);
  }

  /**
   * Creates a new project.
   *
   * @param name the name of a project
   * @param words  words extracted from a secondary corpus object
   * @param <T> element type
   * @return new project object
   */
  public static <T> Project<T> createProject(String name, Set<Word> words){
    return new Project<>(name, words);
  }

  /**
   * Extracts all shared words from a group of projects.
   *
   * @param group the group of projects.
   * @return the set of words shared by all the projects in the
   *    group object.
   */
  public static Set<Word> from(Grouping.Group group){
    final Set<Word> words = Sets.newHashSet();
    Grouping.Group.items(group, Project.class)
      .stream()
      .map(p -> p.words)
      .forEach(words::addAll);

    return words;
  }

  /**
   * Appends another corpus object to the current one.
   *
   * @param other the secondary corpus object.
   */
  public void append(Corpus<T> other){
    corpus().add(other);
  }

  /**
   * @return the name of this project.
   */
  public String name() {
    return name;
  }

  /**
   * @return the most typical words in the project.
   */
  public Set<Word> wordSet() {
    return words;
  }

  /**
   * @return the project's corpus.
   */
  public Corpus<T> corpus() {
    return corpus;
  }

  @Override public String toString() {
    return name();
  }
}
