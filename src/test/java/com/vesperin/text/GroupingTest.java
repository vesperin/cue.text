package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.selection.Tokenizers;
import com.vesperin.text.spelling.StopWords;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class GroupingTest {

  private static List<Word> words;
  private static List<Word> words1;
  private static List<Word> words2;

  private static List<Document> documents;

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

    words  = extractor.weightedWords(100, code, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    words1 = extractor.weightedWords(100, code, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.ENGLISH, StopWords.JAVA)));
    words2 = extractor.weightedWords(100, code, Tokenizers.tokenizeMethodDeclarationName(Collections.emptySet(), StopWords.all()));

    documents = Docs.documents();
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

  @Test public void testClusteringWithUnionFind() throws Exception {
    Grouping.Group g = new Grouping.BasicGroup();
    documents.forEach(g::add);

    final Grouping.Groups gp = Grouping.formDocGroups(g);

    assertThat(gp.isEmpty(), is(false));

    for(Grouping.Group eg : gp){
      final Grouping.Groups gs = Grouping.formDocGroups(eg, 36);

      for(Grouping.Group eachG : gs){
        //assertThat(eachG.itemList().size() < 36, is(true));
        System.out.println(eachG);
      }
    }

  }

  @AfterClass public static void tearDown(){
    words.clear();
    words1.clear();
    words2.clear();
    words  = null;
    words1 = null;
    words2 = null;
    documents.clear();
    documents = null;
  }
}
