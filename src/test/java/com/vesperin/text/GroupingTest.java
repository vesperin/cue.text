package com.vesperin.text;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsTokenizer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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

  @Test public void testProjectClustering() throws Exception {
    final Set<Source> code1 = Sets.newHashSet(
      Codebase.quickSort("QuickSort1", "extends AbstractQuickSort"),
      Codebase.quickSort("QuickSort2", "extends DefaultSorting<String>"),
      Codebase.quickSort("QuickSort3", "implements Sort")
    );

    final Set<Source> code2 = Sets.newHashSet(
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final Set<Source> code3 = Sets.newHashSet(
      Codebase.randomCode("Query1"),
      Codebase.quickSort("QuickSort3")
    );

    final Corpus<Source> c1 = Corpus.ofSources();
    c1.addAll(code1);

    final Corpus<Source> c2 = Corpus.ofSources();
    c2.addAll(code2);

    final Corpus<Source> c3 = Corpus.ofSources();
    c3.addAll(code3);

    final List<Corpus<Source>> corpusList = Lists.newArrayList();
    corpusList.add(c1);
    corpusList.add(c2);
    corpusList.add(c3);

    final WordsTokenizer tokenizer = Tokenizers.tokenizeTypeDeclarationName();

    final List<Project> P = Lists.newArrayList();
    int row = 0;

    for(Corpus<Source> each : corpusList){
      final String name = ("p" + row++);
      final Project project = Project.emptyProject(name);
      project.add(Introspector.frequentWords(each, tokenizer));
      P.add(project);
    }

    final Grouping.Groups groups  = Grouping.groupProjectsBySetIntersection(P);

    assertNotNull(groups);
    assertFalse(groups.isEmpty());
    assertTrue(groups.size() == 2);

    final Grouping.Groups groups2 = Grouping.groupProjectsBySetSimilarity(P);

    assertNotNull(groups2);
    assertFalse(groups2.isEmpty());

    final Grouping.Groups groups3 = Grouping.groupProjectsByKmeans(P);


    assertNotNull(groups3);
    assertFalse(groups3.isEmpty());
    assertTrue(groups3.size() == 1);

    assertEquals(groups.groupList(), groups2.groupList());

    assertNotEquals(groups.groupList(), groups3.groupList());

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
    stopwatch = stopwatch.stop();
    stopwatch = null;
  }
}
