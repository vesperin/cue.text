package com.vesperin.text.nouns;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Huascar Sanchez
 */
final class Rule {
  private final String  exp;
  private final String  replace;
  private final Pattern pattern;

  Rule(String exp, String replace){
    this.exp			= exp;
    this.replace	=	Optional.ofNullable(replace).orElse("");
    this.pattern	= Pattern.compile(this.exp, Pattern.CASE_INSENSITIVE);
  }

  String apply(String word){
    final Optional<String> opt = Optional.ofNullable(word);
    if (!opt.isPresent()) return null;

    final String nonNullInput = opt.get();
    final Matcher matcher = this.pattern.matcher(nonNullInput);
    if (!matcher.find()) return null;
    return matcher.replaceAll(this.replace);
  }

  @Override public boolean equals(Object obj) {
    if(!(obj instanceof Rule)) return false;

    final Rule rule = (Rule) obj;

    return rule.exp.equalsIgnoreCase(exp);

  }

  @Override public int hashCode() {
    return exp.hashCode();
  }

  @Override public String toString() {
    return String.format("(%s)", exp);
  }
}
