package com.vesperin.text.nouns;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
final class Grammar {
  private final LinkedList<Rule>    singular 		= new LinkedList<>();
  private final LinkedList<Rule> 	  plural			= new LinkedList<>();
  private final LinkedList<String>  uncountable = new LinkedList<>();
  private final LinkedList<String>  unchanged   = new LinkedList<>();

  Grammar(){

    plural("$", "s");
    plural("s$", "s");
    plural("(ax|test)is$", "$1es");
    plural("(octop|vir)us$", "$1i");
    plural("(octop|vir)i$", "$1i"); // already plural
    plural("(alias|status)$", "$1es");
    plural("(bu)s$", "$1ses");
    plural("(buffal|tomat)o$", "$1oes");
    plural("([ti])um$", "$1a");
    plural("([ti])a$", "$1a"); // already plural
    plural("sis$", "ses");
    plural("(?:([^f])fe|([lr])f)$", "$1$2ves");
    plural("(hive)$", "$1s");
    plural("([^aeiouy]|qu)y$", "$1ies");
    plural("(x|ch|ss|sh)$", "$1es");
    plural("(matr|vert|ind)ix|ex$", "$1ices");
    plural("([m|l])ouse$", "$1ice");
    plural("([m|l])ice$", "$1ice");
    plural("^(ox)$", "$1en");
    plural("(quiz)$", "$1zes");
    // Need to check for the following words that are already pluralized:
    plural("(people|men|children|sexes|moves|stadiums)$", "$1"); // irregulars
    plural("(oxen|octopi|viri|aliases|quizzes)$", "$1"); // special rules

    singular("s$", "");
    singular("(s|si|u)s$", "$1s"); // '-us' and '-ss' are already singular
    singular("(n)ews$", "$1ews");
    singular("([ti])a$", "$1um");
    singular("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis");
    singular("(^analy)ses$", "$1sis");
    singular("(^analy)sis$", "$1sis"); // already singular, but ends in 's'
    singular("([^f])ves$", "$1fe");
    singular("(hive)s$", "$1");
    singular("(tive)s$", "$1");
    singular("([lr])ves$", "$1f");
    singular("([^aeiouy]|qu)ies$", "$1y");
    singular("(s)eries$", "$1eries");
    singular("(m)ovies$", "$1ovie");
    singular("(x|ch|ss|sh)es$", "$1");
    singular("([m|l])ice$", "$1ouse");
    singular("(bus)es$", "$1");
    singular("(o)es$", "$1");
    singular("(shoe)s$", "$1");
    singular("(cris|ax|test)is$", "$1is"); // already singular, but ends in 's'
    singular("(cris|ax|test)es$", "$1is");
    singular("(octop|vir)i$", "$1us");
    singular("(octop|vir)us$", "$1us"); // already singular, but ends in 's'
    singular("(alias|status)es$", "$1");
    singular("(alias|status)$", "$1"); // already singular, but ends in 's'
    singular("^(ox)en", "$1");
    singular("(vert|ind)ices$", "$1ex");
    singular("(matr)ices$", "$1ix");

    irregular("person", "people");
    irregular("man", "men");
    irregular("child", "children");
    irregular("sex", "sexes");
    irregular("move", "moves");
    irregular("stadium", "stadiums");
    irregular("person", "people");

    uncountable("equipment", "information",
      "rice", "money", "species", "series",
      "fish", "sheep", "advice", "air", "alcohol",
      "art", "advice", "air", "alcohol", "art", "beef",
      "blood", "butter", "coffee", "confusion", "cotton",
      "education", "electricity", "entertainment",
      "experience", "fiction", "flour", "food", "forgiveness",
      "furniture", "gold", "gossip", "grass", "ground",
      "happiness", "history", "homework", "honey", "hop",
      "ice", "information", "knowledge", "lightning",
      "literature", "love", "luck", "luggage", "meat",
      "milk", "mist", "money", "music", "news", "noise",
      "oil", "oxygen", "patience", "pay", "peace", "pepper",
      "petrol", "plastic", "pork", "power", "pressure", "rain",
      "research", "rice", "sadness", "salt", "sand", "shopping",
      "silver", "snow", "space", "speed", "steam",
      "sugar", "sunshine", "tea", "tennis", "thunder",
      "toothpaste", "traffic", "trousers", "vinegar",
      "water", "weather", "wood", "wool", "work");

    unchanged("criteria", "media");
  }

  private static void ensureValidExpression(String exp){
    Objects.requireNonNull(exp);
    if(exp.isEmpty()) throw new IllegalArgumentException("Empty expression");
  }

  private void singular(String exp, String replace){
    ensureValidExpression(exp);

    singular.addFirst(new Rule(exp, replace));
  }

  List<Rule> singularList(){
    return singular;
  }

  private void plural(String exp, String replace){
    ensureValidExpression(exp);
    plural.addFirst(new Rule(exp, replace));
  }

  List<Rule> pluralList(){
    return plural;
  }

  private void irregular(String singular, String plural){

    final String singularRemainder = singular.length() > 1 ? singular.substring(1) : "";
    final String pluralRemainder = plural.length() > 1 ? plural.substring(1) : "";
    plural("(" + singular.charAt(0) + ")" + singularRemainder + "$", "$1" + pluralRemainder);
    singular("(" + plural.charAt(0) + ")" + pluralRemainder + "$", "$1" + singularRemainder);
  }

  boolean isUncountable(String word){
    if (word == null) return false;
    final String trimmedLower = word.trim().toLowerCase();
    return uncountable.contains(trimmedLower);
  }

  boolean isUnchanged(String word){
    if (word == null) return false;
    final String trimmedLower = word.trim().toLowerCase();
    return unchanged.contains(trimmedLower);
  }

  private void uncountable(String... words){
    if(words == null || words.length == 0) return;

    final List<String> filtered = Arrays.asList(words).stream()
      .filter(w -> null != w)
      .collect(toList());

    uncountable.addAll(filtered);
  }

  private void unchanged(String... words){
    if(words == null || words.length == 0) return;

    final List<String> filtered = Arrays.asList(words).stream()
      .filter(w -> null != w)
      .collect(toList());

    unchanged.addAll(filtered);
  }

  void clear(){
    if(!this.singular.isEmpty()) this.singular.clear();
    if(!this.plural.isEmpty()) this.plural.clear();
    if(!this.uncountable.isEmpty()) this.uncountable.clear();
  }
}
