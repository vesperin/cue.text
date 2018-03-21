package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.tokenizers.WordsTokenizer;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Project {
  private final String    name;
  private final Set<Word> words;

  private Project(String name, Set<Word> words){
    this.name   = name;
    this.words  = words;
  }

  /**
   * Creates an empty project; that is a project with no words.
   * These words should be gathered using either
   * {@link Introspector#frequentWords(Corpus, WordsTokenizer)} or
   * {@link Introspector#typicalityRank(Corpus, WordsTokenizer)} or
   * {@link Introspector#typicalityQuery(int, Corpus, WordsTokenizer)}.
   *
   * @param name name of project.
   * @return a new project object.
   */
  public static Project emptyProject(String name){
    return createProject(name, Sets.newHashSet());
  }

  /**
   * Creates a new project.
   *
   * These words should be gathered using either
   * {@link Introspector#frequentWords(Corpus, WordsTokenizer)} or
   * {@link Introspector#typicalityRank(Corpus, WordsTokenizer)} or
   * {@link Introspector#typicalityQuery(int, Corpus, WordsTokenizer)}.
   *
   * @param name the name of a project
   * @param words  words extracted from a secondary corpus object
   * @return new project object
   */
  public static Project createProject(String name, Set<Word> words){
    return new Project(name, words);
  }

  /**
   * Adds a word to this project.
   * @param word word object to add
   */
  public void add(Word word){
    this.words.add(word);
  }

  /**
   * Adds a set of words to this project.
   * @param words word set to add
   */
  public void add(Collection<Word> words){
    if(Objects.isNull(words)) return;
    if(words.contains(null)) return;

    words.forEach(this::add);
  }

  /**
   * Extracts all shared words from a group of projects.
   *
   * @param group the group of projects.
   * @return the set of words shared by all the projects in the
   *    group object.
   */
  public static Set<Word> toWords(List<Project> group){
    final Set<Word> words = Sets.newHashSet();
    group.stream()
      .map(p -> p.words)
      .forEach(words::addAll);

    return words;
  }

  @Override public int hashCode() {
    return Objects.hash(name(), wordSet());
  }

  @Override public boolean equals(Object obj) {
    if(!(obj instanceof Project)) return false;

    final Project other = (Project) obj;

    final boolean sameName  = other.name().equals(name());
    final boolean sameWords = wordSet().equals(other.wordSet());

    return sameName && sameWords;
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

  @Override public String toString() {
    return String.format("%s with %d words", name(), wordSet().size());
  }
}
