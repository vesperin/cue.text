package com.vesperin.text.tokenizers;

import com.vesperin.text.Selection;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.nouns.Noun;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Similarity;
import com.vesperin.text.utils.Strings;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
   * Process an array of raw tokens before adding them to a list of words.
   *
   * @param tokens raw tokens
   */
  default void process(String[] tokens, String from){
    final Set<String> distinctTokens = Strings.intersect(tokens, tokens);

    for (String eachToken : distinctTokens) {
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
