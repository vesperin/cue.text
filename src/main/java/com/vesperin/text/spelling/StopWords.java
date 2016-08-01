package com.vesperin.text.spelling;

import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public enum StopWords {
  ENGLISH(), JAVA();


  private static final String CUSTOM_WORD = "custom";

  public final boolean stripApostrophes;
  private final Set<String> stopWords;

  /**
   * Construct the StopWords enum
   */
  StopWords() {
    this(false);
  }

  /**
   * Construct the StopWords enum
   *
   * @param stripApostrophes s
   */
  StopWords(boolean stripApostrophes) {
    this.stripApostrophes = stripApostrophes;
    this.stopWords = new HashSet<>();

    loadSupportedLanguages();
  }

  /**
   * Adds a new word to the stop-words list.
   *
   * @param word new word to add
   */
  public void add(String word){
    final String nonNullWord = Objects.requireNonNull(word);
    stopWords.add(nonNullWord.toLowerCase(Locale.ENGLISH));
  }

  /**
   * @return a set of all currently available
   *  StopWords
   */
  public static Set<StopWords> all(){
    return Sets.newHashSet(ENGLISH, JAVA);
  }

  /**
   * Test if a string is a member of any of the given sets of stop words.
   *
   * @param corpus the corpus of stop words.
   * @param word the string to be tested.
   * @return true if the string is a stop word; false otherwise.
   */
  public static boolean isStopWord(Set<StopWords> corpus, String... word){
    for(String w : word){
      for(StopWords s : corpus){
        if(s.isStopWord(w)) return true;
      }
    }

    return false;
  }

  /**
   * Tests if a word is a stop word.
   * @param word the word to be tested.
   * @return true if the word is a stop word; false otherwise.
   */
  public boolean isStopWord(final String word) {
    if (word.length() == 1) {
      return true;
    }
    // check right quotes as apostrophes
    return stopWords.contains(
      word.replace('\u2019', '\'').toLowerCase(Locale.ENGLISH)
    );
  }

  private void loadSupportedLanguages() {
    final String wordListResource = name().toLowerCase(Locale.ENGLISH);
    if (!CUSTOM_WORD.equals(wordListResource)) {

      final Class<?> stopWordsClass = getClass();

      try (final InputStream in = stopWordsClass.getResourceAsStream("/" + wordListResource);
           final InputStreamReader inr = new InputStreamReader(in, Charset.forName("UTF-8"))) {

        final List<String> lines = CharStreams.readLines(inr);
        final Iterator<String> iterator = lines.iterator();
        String line;

        while (iterator.hasNext()) {
          line = iterator.next();
          line = line.replaceAll("\\|.*", "").trim();

          if (line.length() == 0) {
            continue;
          }

          for (final String w : line.split("\\s+")) {
            add(w);
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}