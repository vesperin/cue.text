package com.vesperin.text.spelling;

import com.vesperin.text.utils.Ios;
import com.vesperin.text.utils.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

import static com.vesperin.text.utils.Strings.isNumber;
import static com.vesperin.text.utils.Strings.onlyConsonantsOrVowels;

/**
 * @author Huascar Sanchez
 */
public enum Dictionary implements WordKeeper<String> {
  ENGLISH();

  private final List<String> words;

  /**
   * Construct the Dictionary enum
   */
  Dictionary(){
    this.words = new ArrayList<>();

    try {
      wordCollection(loadBuiltInDictionary(), this.words);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to populate dictionary!");
    }
  }


  @Override public void add(String word) {
    final String nonNullWord = Objects.requireNonNull(word);
    words.add(nonNullWord.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Test if a string is a member of any of the given sets of stop words.
   *
   * @param word the string to be tested.
   * @return true if the string is a stop word; false otherwise.
   */
  public static boolean isDefined(String... word){
    for(String w : word){
      if(Dictionary.ENGLISH.contains(w) && !Strings.isNumber(w)) return true;
    }

    return false;
  }

  /**
   * Tests if a word is a stop word.
   * @param word the word to be tested.
   * @return true if the word is a stop word; false otherwise.
   */
  public boolean contains(final String word) {
    if (word.length() == 1) {
      return true;
    }
    // check right quotes as apostrophes
    return words.contains(
      word.replace('\u2019', '\'').toLowerCase(Locale.ENGLISH)
    );
  }

  private static Path loadBuiltInDictionary(){
    try {
      final Path systemDict = Paths.get("/usr/share/dict/words");
      if(Ios.exists(systemDict)){
        return systemDict;
      }
    } catch (Exception ignored){
      System.err.println("Unable to access the system's dictionary.");
    }

    final Path backup = Ios.loadFile("dict.txt");
    if(Ios.exists(backup)){
      return backup;
    }

    throw new NoSuchElementException("Unable to find dictionary");
  }

  private static void wordCollection(Path dictionaryFile, List<String> dict) throws IOException {
    Objects.requireNonNull(dict);
    Objects.requireNonNull(dictionaryFile);

    final List<String> lines = Files.readAllLines(dictionaryFile);

    for(String line : lines){

      if(line.length() <= 2)            continue;
      if(isNumber(line))                continue;
      if(onlyConsonantsOrVowels(line))  continue;

      dict.add(line); // each line is a word
    }
  }
}