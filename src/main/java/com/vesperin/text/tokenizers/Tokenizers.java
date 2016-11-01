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


  /**
   *
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeTypeDeclarationName(){
    return new ClassNameWordsTokenizer(Collections.emptySet(), Collections.emptySet());
  }

  /**
   *
   * @param stopWords
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeTypeDeclarationName(Set<StopWords> stopWords){
    return new ClassNameWordsTokenizer(Collections.emptySet(), stopWords);
  }

  /**
   *
   * @param whiteSet
   * @param stopWords
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeTypeDeclarationName(Set<String> whiteSet, Set<StopWords> stopWords){
    return new ClassNameWordsTokenizer(whiteSet, stopWords);
  }



  /**
   *
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeMethodDeclarationName(){
    return new MethodNameWordsTokenizer(Collections.emptySet(), Collections.emptySet());
  }

  /**
   *
   * @param stopWords
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeMethodDeclarationName(Set<StopWords> stopWords){
    return new MethodNameWordsTokenizer(Collections.emptySet(), stopWords);
  }

  /**
   *
   * @param whiteSet
   * @param stopWords
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeMethodDeclarationName(Set<String> whiteSet, Set<StopWords> stopWords){
    return new MethodNameWordsTokenizer(whiteSet, stopWords);
  }


  /**
   *
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeMethodDeclarationBody(){
    return new MethodBodyWordsTokenizer(Collections.emptySet(), Collections.emptySet());
  }

  /**
   *
   * @param stopWords
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeMethodDeclarationBody(Set<StopWords> stopWords){
    return new MethodBodyWordsTokenizer(Collections.emptySet(), stopWords);
  }

  /**
   *
   * @param whiteSet
   * @param stopWords
   * @return
   */
  public static ASTNodeWordsTokenizer tokenizeMethodDeclarationBody(Set<String> whiteSet, Set<StopWords> stopWords){
    return new MethodBodyWordsTokenizer(whiteSet, stopWords);
  }


}
