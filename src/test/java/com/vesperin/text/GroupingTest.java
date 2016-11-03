package com.vesperin.text;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
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

  private static Stopwatch stopwatch;

  @BeforeClass public static void setup(){

    stopwatch = Stopwatch.createStarted();

    final Selection<Source> extractor = new WordDistilling<>();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(code);
    System.out.println("initializing corpus: " + stopwatch);

    words  = extractor.topKWords(100, corpus, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    System.out.println("loading words from method bodies: " + stopwatch);

    words1 = extractor.topKWords(100, corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.ENGLISH, StopWords.JAVA)));
    System.out.println("loading words from class names: " + stopwatch);

    words2 = extractor.topKWords(100, corpus, Tokenizers.tokenizeMethodDeclarationName(Collections.emptySet(), StopWords.all()));
    System.out.println("loading words from method names: " + stopwatch);

    documents = Docs.documents();
    System.out.println("loading documents: " + stopwatch);
  }

  @Test public void testWordGrouping() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.ofWords(words);

    System.out.println("testWordGrouping: " + stopwatch);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testWordGrouping1() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.ofWords(words1);
    System.out.println("testWordGrouping1: " + stopwatch);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testWordGrouping2() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.ofWords(words2);
    System.out.println("testWordGrouping2: " + stopwatch);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testDocGrouping() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.ofDocs(words);
    System.out.println("testDocGrouping: " + stopwatch);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }


  @Test public void testDocGrouping1() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.ofDocs(words1);
    System.out.println("testDocGrouping1: " + stopwatch);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }


  @Test public void testDocGrouping2() throws Exception {
    final Grouping grouping = new WordGrouping();
    final Grouping.Groups groups = grouping.ofDocs(words1);
    System.out.println("testDocGrouping2: " + stopwatch);

    assertTrue(!groups.isEmpty());

    System.out.println(groups);

  }

  @Test public void testClusteringWithUnionFind() throws Exception {
    Grouping.Group g = Grouping.newGroup();
    documents.forEach(g::add);

    System.out.println("testClusteringWithUnionFind(adding docs to group): " + stopwatch);

    final Grouping.Groups gp = Grouping.regroups(g);
    System.out.println("testClusteringWithUnionFind(grouping docs): " + stopwatch);

    assertThat(gp.isEmpty(), is(false));

    for(Grouping.Group eg : gp){
      final Grouping.Groups gs = Grouping.regroups(eg, 36);

      for(Grouping.Group eachG : gs){
        //assertThat(eachG.itemList().size() < 36, is(true));
        System.out.println(eachG);
      }
    }

    System.out.println("testClusteringWithUnionFind(printing groups): " + stopwatch);

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
    System.out.println("resetting everything: " + stopwatch);
    stopwatch.stop();
    stopwatch = null;
  }
}
