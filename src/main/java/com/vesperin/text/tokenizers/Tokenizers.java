package com.vesperin.text.tokenizers;

import com.vesperin.text.spelling.StopWords;

import java.util.Collections;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
public class Tokenizers {
  private Tokenizers(){
    throw new Error("Cannot be instantiated!");
  }


  public static boolean isMethodNameTokenizer(WordsTokenizer tokenizer){
    return tokenizer instanceof WordsInMethodNameTokenizer;
  }

  public static boolean isClassNameTokenizer(WordsTokenizer tokenizer){
    return tokenizer instanceof WordsInClassNameTokenizer;
  }

  /**
   * @return a new {@link WordsInClassNameTokenizer} object.
   */
  public static WordsInASTNodeTokenizer tokenizeTypeDeclarationName(){
    return new WordsInClassNameTokenizer(Collections.emptySet(), Collections.emptySet());
  }

  /**
   * Creates a new {@link WordsInClassNameTokenizer} object given a set of stopwords.
   *
   * @param stopWords used stop words set.
   * @return a new {@link WordsInClassNameTokenizer} object.
   */
  public static WordsInASTNodeTokenizer tokenizeTypeDeclarationName(Set<StopWords> stopWords){
    return new WordsInClassNameTokenizer(Collections.emptySet(), stopWords);
  }

  /**
   * Creates a new {@link WordsInClassNameTokenizer} object given a set of
   * permitted words and a set of stopwords.
   *
   * @param whiteSet set of permitted words
   * @param stopWords set of stop words.
   * @return a new {@link WordsInClassNameTokenizer} object.
   */
  public static WordsInASTNodeTokenizer tokenizeTypeDeclarationName(Set<String> whiteSet, Set<StopWords> stopWords){
    return new WordsInClassNameTokenizer(whiteSet, stopWords);
  }



  /**
   * @return a new {@link WordsInMethodNameTokenizer} object.
   */
  public static WordsInASTNodeTokenizer tokenizeMethodDeclarationName(){
    return new WordsInMethodNameTokenizer(Collections.emptySet(), Collections.emptySet());
  }

  /**
   * Creates a new {@link WordsInMethodNameTokenizer} object.
   *
   * @param stopWords set of stop words.
   * @return a new {@link WordsInMethodNameTokenizer} object.
   */
  public static WordsInASTNodeTokenizer tokenizeMethodDeclarationName(Set<StopWords> stopWords){
    return new WordsInMethodNameTokenizer(Collections.emptySet(), stopWords);
  }

  /**
   * Creates a new {@link WordsInMethodNameTokenizer} object.
   *
   * @param whiteSet set of permitted words
   * @param stopWords set of stop words
   * @return a new {@link WordsInMethodNameTokenizer} object.
   */
  public static WordsInASTNodeTokenizer tokenizeMethodDeclarationName(Set<String> whiteSet, Set<StopWords> stopWords){
    return new WordsInMethodNameTokenizer(whiteSet, stopWords);
  }


  /**
   *
   * @return
   */
  public static WordsInASTNodeTokenizer tokenizeMethodDeclarationBody(){
    return new WordsInMethodBodyTokenizer(Collections.emptySet(), Collections.emptySet());
  }

  /**
   *
   * @param stopWords
   * @return
   */
  public static WordsInASTNodeTokenizer tokenizeMethodDeclarationBody(Set<StopWords> stopWords){
    return new WordsInMethodBodyTokenizer(Collections.emptySet(), stopWords);
  }

  /**
   *
   * @param whiteSet
   * @param stopWords
   * @return
   */
  public static WordsInASTNodeTokenizer tokenizeMethodDeclarationBody(Set<String> whiteSet, Set<StopWords> stopWords){
    return new WordsInMethodBodyTokenizer(whiteSet, stopWords);
  }

  /**
   *
   * @return
   */
  public static WordsInStringTokenizer tokenizeString(){
    return new WordsInStringTokenizer(Collections.emptySet());
  }

  /**
   *
   * @param stopWords
   * @return
   */
  public static WordsInStringTokenizer tokenizeString(Set<StopWords> stopWords){
    return new WordsInStringTokenizer(stopWords);
  }


}
