package com.vesperin.text;

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
    this.words  = Selection.typicalWords(corpus, tokenizer)
      .stream().collect(Collectors.toSet());
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
