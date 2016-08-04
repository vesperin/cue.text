package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class GroupingTest {

  private static List<Word> words;

  @BeforeClass public static void setup(){
    final Selection extractor = new WordDistilling();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    words = extractor.weightedWords(100, code, Collections.emptySet(), StopWords.all());
  }

  @Test public void testWordGrouping() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.groups(words);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

//  @Test public void testDocGrouping() throws Exception {
//    final Grouping grouping = new WordGrouping();
//    final Grouping.Groups groups = grouping.docGroups(words);
//
//    assertTrue(!groups.isEmpty());
//
//    System.out.println(groups);
//
//  }

  @AfterClass public static void tearDown(){
    words.clear();
    words = null;
  }
}
