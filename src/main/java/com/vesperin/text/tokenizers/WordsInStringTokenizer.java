package com.vesperin.text.tokenizers;

import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
class WordsInStringTokenizer implements WordsTokenizer {
  private final Set<StopWords>  stopWords;
  private final List<Word>      words;

  WordsInStringTokenizer(Set<StopWords> stopWords){
    this.stopWords  = stopWords;
    this.words      = new ArrayList<>();
  }

  @Override public void clear() {
    wordsList().clear();
  }

  @Override public List<Word> wordsList() {
    return words;
  }

  @Override public Set<StopWords> stopWords() {
    return stopWords;
  }

  @Override public boolean isLightweightTokenizer() {
    return true;
  }

  @Override public String[] tokenize(String text) {

    final int idx = text.lastIndexOf("$");
    text = idx > 0 ? text.substring(0, idx) : text;

    return Strings.wordSplit(text);
  }

  @Override public String toString() {
    return wordsList().toString();
  }
}
