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
  private static List<Word> words1;
  private static List<Word> words2;

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

    words  = extractor.weightedWords(100, code, Selection.inspectMethodBody(Collections.emptySet(), StopWords.all()));
    words1 = extractor.weightedWords(100, code, Selection.inspectClassName(Collections.emptySet(), StopWords.of(StopWords.ENGLISH, StopWords.JAVA)));
    words2 = extractor.weightedWords(100, code, Selection.inspectMethodName(Collections.emptySet(), StopWords.all()));
  }

  @Test public void testWordGrouping() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.wordGroup(words);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testWordGrouping1() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.wordGroup(words1);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testWordGrouping2() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.wordGroup(words2);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testDocGrouping() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.docGroups(words);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }


  @Test public void testDocGrouping1() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.docGroups(words1);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }


  @Test public void testDocGrouping2() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.docGroups(words1);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @AfterClass public static void tearDown(){
    words.clear();
    words1.clear();
    words2.clear();
    words  = null;
    words1 = null;
    words2 = null;
  }
}
