package com.vesperin.text.tokenizers;

import com.vesperin.text.Selection;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.nouns.Noun;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vesperin.text.spelling.SpellCorrector.suggestCorrection;

/**
 * @author Huascar Sanchez
 */
public interface WordsTokenizer extends Iterable<Word> {
  /**
   * Clears tokenizer.
   */
  void clear();

  /**
   * @return the current list of words.
   */
  List<Word> wordsList();

  /**
   * @return the current set of stop words.
   */
  Set<StopWords> stopWords();

  /**
   * @return true if this tokenizer requires a non parsed representation
   *    of the words container; false otherwise.
   */
  boolean isLightweightTokenizer();

  /**
   * Tokenize a given text.
   *
   * @param text text to be tokenized.
   * @return an array of Strings
   */
  default String[] tokenize(String text){
    final List<String> splits = Arrays.stream(text.split(Pattern.quote("$")))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    final String prunedText = splits.get(0); // left side of the text

    return Strings.wordSplit(prunedText);
  }

  /**
   * Tokenize text and then retains only the substrings in this text that
   * are contained in the specified alternative text.
   *
   * @param text actual text
   * @param alternative super class text
   * @param container actual Java class (fully qualified name)
   */
  default void tokenize(String text, String alternative, String container){
    if (!skipThrowablesAlike(text) && !skipThrowablesAlike(alternative)) {
      // make sure we have a valid split

      final String[] x = tokenize(text);
      final String[] y = tokenize(alternative);

      Set<String> a = Arrays.stream(x).collect(Collectors.toSet());
      Set<String> b = Arrays.stream(y).collect(Collectors.toSet());

      a.retainAll(b);

      final String[] c = (!a.isEmpty()) ? a.toArray(new String[a.size()]) : x;

      // process all split tokens
      process(c, container);
    }
  }

  /**
   * Tokenize some text extracted from either a given container or the body of
   * a given container.
   *
   * @param text raw text
   * @param container either a class name or a method name.
   */
  default void tokenize(String text, String container){
    if (!skipThrowablesAlike(text)) {
      // make sure we have a valid split
      String[] split = tokenize(text);

      // process all split tokens
      process(split, container);
    }
  }

  static boolean skipThrowablesAlike(String identifier) {
    return (identifier.endsWith("Exception")
      || identifier.equals("Throwable")
      || identifier.equals("Error"));
  }

  /**
   * Process an array of raw tokens before adding them to a list of words.
   *
   * @param tokens raw tokens
   */
  default void process(String[] tokens, String from){
    for (String eachToken : tokens) {
      if (skipStopWords(eachToken, stopWords())) continue;

      String token = eachToken.toLowerCase(Locale.ENGLISH);
      token        = processIfNeeded(token);

      final Selection.Word word = Selection.createWord(token);
      word.add(from);

      wordsList().add(word);
    }
  }

  @Override default Iterator<Word> iterator() {
    return wordsList().iterator();
  }


  static boolean skipStopWords(String token, Set<StopWords> stops){
    return token == null
      || (" ".equals(token)
      || token.isEmpty())
      || (StopWords.isStopWord(stops, token, Noun.get().pluralOf(token))
    );
  }

  static String processIfNeeded(String token){
    final String newLabel = suggestCorrection(token).toLowerCase(Locale.ENGLISH);

    if (Similarity.jaccard(token, newLabel) > 0.3D) {
      token = newLabel;
    }

    if(isUppercase(token)){
      token = token.toLowerCase(Locale.ENGLISH);
    }

    return token;
  }

  static boolean isUppercase(String token){
    return token != null && Character.isUpperCase(token.charAt(0));
  }
}
