package com.vesperin.text.spelling;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
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
  ENGLISH(), JAVA(), GENERAL(), CUSTOM();


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
   * Adds a list of words to the stop-words object.
   *
   * @param words list of words to add
   */
  public void addAll(List<String> words){
    final Set<String> uniqueWords = new HashSet<>();
    uniqueWords.addAll(words);

    for(String each : uniqueWords){
      if(Objects.isNull(each) || each.isEmpty()) continue;
      final String lowercase = each.toLowerCase(Locale.ENGLISH);
      add(lowercase);
    }
  }

  /**
   * @return a constructed set of stop word objects
   */
  public static Set<StopWords> of(StopWords... stopWords){

    if(Objects.isNull(stopWords) || stopWords.length == 0) return Collections.emptySet();

    Set<StopWords> result = null;
    for (StopWords each : stopWords){
      if(Objects.isNull(result)) { result = EnumSet.of(each); } else {
        result.add(each);
      }
    }

    return result;

  }

  /**
   * @return a set of all currently available
   *  StopWords
   */
  public static Set<StopWords> all(){
    return EnumSet.of(StopWords.ENGLISH, StopWords.JAVA, StopWords.GENERAL);

  }

  /**
   * Refreshes all the relevant stop words with a fresh list of words.
   * @param english english list of words
   * @param java java list of words
   * @param general general programming list of words
   * @return a new set of refreshed stop words.
   */
  public static Set<StopWords> update(List<String> english, List<String> java, List<String> general){
    StopWords.ENGLISH.addAll(english);
    StopWords.JAVA.addAll(java);
    StopWords.GENERAL.addAll(general);

    return all();
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
        if(s.isStopWord(w) || Corrector.isNumber(w)) return true;
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