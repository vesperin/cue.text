package com.vesperin.text.nouns;

import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class Noun {
  private final Grammar grammar;

  private Noun(){
    this.grammar = new Grammar();
  }

  public static Noun newNoun(){
    return new Noun();
  }


  boolean isSingular(String word){
    return Objects.equals(singularOf(word), word);
  }

  boolean isPlural(String word){
    return Objects.equals(pluralOf(word), word);
  }


  public String pluralOf(String word){
    if(grammar.isUncountable(word)) return word;
    return applyRule(grammar.pluralList(), word);
  }

  public String singularOf(String word){
    if(grammar.isUncountable(word)) return word;
    return applyRule(grammar.singularList(), word);
  }

  private static String applyRule(List<Rule> rules, String word){
    for(Rule each : rules){
      final String result = each.apply(word);
      if (result != null) return result;
    }

    return word;
  }

  void clearGrammar(){
    grammar.clear();
  }

}
