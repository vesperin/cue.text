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

  public static Noun get() {
    return Installer.NOUN;
  }

  public static String toSingular(String word){
    final Noun instance = Noun.get();
    return instance.isPlural(word) ? instance.singularOf(word) : word;
  }

  public boolean isSingular(String word){
    return Objects.equals(singularOf(word), word);
  }

  public boolean isPlural(String word){
    return Objects.equals(pluralOf(word), word);
  }


  public String pluralOf(String word){
    if(grammar.isUncountable(word)) return word;
    return applyRule(grammar.pluralList(), word);
  }

  public String singularOf(String word){
    if(grammar.isUncountable(word)) return word;
    if(grammar.isUnchanged(word)) return word;
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


  private static class Installer {
    static final Noun NOUN = Noun.newNoun();
  }

}
