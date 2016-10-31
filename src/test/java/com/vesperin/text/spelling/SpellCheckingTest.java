package com.vesperin.text.spelling;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.vesperin.text.spelling.SpellCorrector.suggestCorrection;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class SpellCheckingTest {
  @Test public void testSpellChecking() throws Exception {
    final Set<String> corrections = new HashSet<>();
    corrections.add("configuration");
    corrections.add("text");
    corrections.add("error");
    corrections.add("string");

    final Set<String> words = new HashSet<>();
    words.add("txt");
    words.add("config");
    words.add("err");
    words.add("str");

    for(String each : words){
      final String correction = suggestCorrection(each);
      System.out.println(correction);
       assertThat(corrections.contains(correction), is(true));
    }
  }
}
