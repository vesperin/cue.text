package com.vesperin.text.tokenizers;

import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;

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

  @Override public String toString() {
    return wordsList().toString();
  }
}
