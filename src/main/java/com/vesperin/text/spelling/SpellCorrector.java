package com.vesperin.text.spelling;

import com.vesperin.text.nouns.Noun;
import com.vesperin.text.spi.BasicExecutionMonitor;
import com.vesperin.text.utils.Ios;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vesperin.text.utils.Strings.isNumber;
import static com.vesperin.text.utils.Strings.onlyConsonantsOrVowels;

/**
 * @author Huascar Sanchez
 */
public enum SpellCorrector implements Corrector {
  INSTANCE(Ios.loadFile("big.txt"));

  private static final String CAMEL_CASE = "((?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z]))|_";

  private SortedMap<String,Integer> wordToFrequency;

  SpellCorrector(Path index){
    this.wordToFrequency = new TreeMap<>();

    if(!Objects.isNull(index) && Files.exists(index)) {
      try {
        populateDictionary(index, this.wordToFrequency);
        BasicExecutionMonitor.get().info("big.txt has been loaded");
      } catch (IOException e) {
        BasicExecutionMonitor.get().error("big.txt has not been loaded", e);
        throw new IllegalStateException("Unable to populate dictionary. Cannot find big.txt file!");
      }
    } else {
      populateDictionary(TextFailover.DEFAULT_TEXT, this.wordToFrequency);
      BasicExecutionMonitor.get().info("big.txt could not been found; failover text has been used instead.");
    }

  }


  public static SpellCorrector getInstance(){
    return SpellCorrector.INSTANCE;
  }

  /**
   * Suggests a correction for a given word.
   *
   * @param word the string to be corrected.
   * @return a suggested correction.
   */
  public static String suggestCorrection(String word){
    return word; //SpellCorrector.getInstance().correct(word);
  }


  @Override public String correct(String word, float accuracy) {

    if(contains(word)) { return word; } else {
      if(onlyConsonantsOrVowels(word)) {

        // Make some edits
        final Optional<String> e1 = max(captureThoseInDictionary(mutate(word)));
        final Optional<String> e2 = max(captureThoseInDictionary(mutate(word)
          .map(this::mutate).flatMap((x)->x))
        );

        // Use prefixes (like a Trie)
        Optional<String> e3 = max(getPrefixedBy(word, wordToFrequency).stream());
        if(!e3.isPresent()){
          e3 = max(getPrefixedBy(word.substring(0, word.length() - 1), wordToFrequency).stream());
        }

        final Set<String> winners = new HashSet<>();
        e1.ifPresent(winners::add);
        e2.ifPresent(winners::add);
        e3.ifPresent(winners::add);

        final Optional<String> winner = winners.stream().max(
          Comparator.comparingDouble(a -> Similarity.jaccard(word, a))
        );

        return winner.orElse(word);

      } else {
        Optional<String> e0 = max(getPrefixedBy(word, wordToFrequency).stream());

        if(!e0.isPresent()){
          e0 = max(getPrefixedBy(word.substring(0, word.length() - 1), wordToFrequency).stream());
        }

        return e0.orElse(word);

      }
    }
  }


  private Stream<String> captureThoseInDictionary(Stream<String> words){
    return words.filter(this::contains);
  }

  public static boolean containsWord(String word){
    return SpellCorrector.getInstance().contains(word);
  }

  private boolean contains(String word){
    return wordToFrequency.containsKey(word);
  }

  private static <V> SortedMap<String, V> filterPrefix(SortedMap<String,V> baseMap, String prefix) {
    if(prefix.length() > 0) {
      char nextLetter = (char)(prefix.charAt(prefix.length() - 1) + 1);
      String end = prefix.substring(0, prefix.length() - 1) + nextLetter;
      return baseMap.subMap(prefix, end);
    }
    return baseMap;
  }

  private static <V> Set<String> getPrefixedBy(String word, SortedMap<String, V> baseMap){
    return filterPrefix(baseMap, word).keySet();
  }


  private Optional<String> max(Stream<String> stream){
    return stream.max(Comparator.comparingInt(a -> wordToFrequency.get(a)));
  }

  private Stream<String> mutate(final String word){
    final Stream<String> deletes    = IntStream.range(0, word.length())
      .mapToObj((i) -> word.substring(0, i) + word.substring(i + 1));

    final Stream<String> replaces   = IntStream.range(0, word.length())
      .mapToObj((i)->i)
      .flatMap( (i) -> "abcdefghijklmnopqrstuvwxyz".chars()
        .mapToObj( (c) -> word.substring(0,i) + (char)c + word.substring(i+1)));

    final Stream<String> inserts    = IntStream.range(0, word.length()+1)
      .mapToObj((i)->i)
      .flatMap( (i) -> "abcdefghijklmnopqrstuvwxyz".chars()
        .mapToObj( (c) ->  word.substring(0,i) + (char)c + word.substring(i)));

    final Stream<String> transposes = IntStream.range(0, word.length()-1)
      .mapToObj((i)-> word.substring(0,i)
        + word.substring(i + 1,i + 2)
        + word.charAt(i) + word.substring(i + 2));

    return Stream.of( deletes,replaces,inserts,transposes ).flatMap((x)->x);
  }


  private static void populateDictionary(Path dictionaryFile, SortedMap<String, Integer> dict) throws
    IOException {
    Objects.requireNonNull(dict);
    Objects.requireNonNull(dictionaryFile);

    final List<String> lines = Files.readAllLines(dictionaryFile);
    processLines(lines, dict);
  }

  private static void populateDictionary(List<String> lines, SortedMap<String, Integer> dict){
    processLines(lines, dict);
  }

  private static void processLines(List<String> lines, SortedMap<String, Integer> dict) {
    Pattern p = Pattern.compile("\\w+");
    for(String line : lines){
      final Matcher m = p.matcher(line);

      while(m.find()) {

        final String[] words = m.group().split(CAMEL_CASE);

        for(String each : words){

          if(each.length() <= 2)            continue;
          if(onlyConsonantsOrVowels(each))  continue;
          if(isNumber(each))                continue;

          String updatedEach = Strings.trimSideNumbers(each, true);
          updatedEach        = Noun.get().isPlural(updatedEach)
            ? Noun.get().singularOf(updatedEach)
            : updatedEach;

          dict.put(
            updatedEach,
            // increase frequency
            (dict.containsKey(updatedEach) ? dict.get(updatedEach) + 1 : 1)
          );

        }
      }
    }
  }


}
