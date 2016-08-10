package com.vesperin.text.nouns;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class NounTest {

  private static Noun noun;

  @BeforeClass public static void setup() throws Exception {
    noun = Noun.newNoun();
  }

  @Test public void testSingularToPluralRules() throws Exception {
    final String peaches = "peaches";

    final String plural  = toPlural("peach");

    assertEquals(peaches, plural);

  }

  @Test public void testPluralToPluralRules() throws Exception {
    final String peaches = "peaches";

    final String plural  = toPlural("peaches");

    assertEquals(peaches, plural);

  }

  @Test public void testPluralToSingularRulesUnchanged() throws Exception {
    final String criteria = "criteria";

    final String singular  = toSingular("criteria");

    assertEquals(criteria, singular); // should remain unchanged

  }

  @Test public void testIsPlural() throws Exception {
    assertTrue(noun.isPlural("nouns"));
  }


  @Test public void testPluralToSingularRules() throws Exception {
    final String peach = "peach";

    final String singular  = toSingular("peaches");

    assertEquals(peach, singular);

  }

  @Test public void testSingularToSingularRules() throws Exception {
    final String peach = "peach";

    final String singular = toSingular("peach");

    assertEquals(peach, singular);

  }

  @Test public void testIsSingular() throws Exception {
    assertTrue(noun.isSingular("estimate"));
  }


  private static String toPlural(String word){
    return noun.pluralOf(word);
  }

  private static String toSingular(String word){
    return noun.singularOf(word);
  }

  @AfterClass public static void tearDown() throws Exception {
    if(noun != null){
      noun.clearGrammar();
      noun = null;
    }
  }
}
