package com.vesperin.text;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;

/**
 * @author Huascar Sanchez
 */
public class UsecaseTest {

  private static List<Word>     words;
  private static Set<StopWords> stopWords;
  private static List<Word>     targets;

  private static Stopwatch      stopwatch;

  @BeforeClass public static void setup() throws Exception {
    stopwatch = Stopwatch.createStarted();

    stopWords = StopWords.all();
    System.out.println("loading stopwords: " + stopwatch);

    final Set<Source> sources = allSourceFiles();
    System.out.println("loading source files: " + stopwatch);

    Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(sources);

    System.out.println("moving files to corpus: " + stopwatch);

    final Map<List<Word>, List<Word>> mapping = Selection.frequentToTypicalMapping(corpus, Tokenizers.tokenizeTypeDeclarationName(stopWords));

    words   = Iterables.get(mapping.keySet(), 0).stream().limit(50).collect(Collectors.toList());

    System.out.println("collecting top 50 frequent words: " + stopwatch);

    targets = Iterables.get(mapping.values(), 0);

    System.out.println("identifying the most typical words: " + stopwatch);
  }

  @Test public void systemTest0() throws Exception {

    final Grouping.Groups groups = Grouping.regroupDocs(words);
    assertNotNull(groups);
  }

  @Test public void systemTest1() throws Exception {

    final Map<Grouping.Group, Index> mapping = Grouping.buildGroupIndexMapping(words);
    final Grouping.Groups groups = Grouping.regroupDocs(mapping);

    final Index index = groups.index();

    for(Grouping.Group each : groups){
      final List<Document> documents = Grouping.Group.items(each, Document.class);
      final Query.Result result  = Query.words(documents, index);

      final List<String> result1 = Recommend.labels(documents);

      System.out.println(Document.names(documents));
      System.out.println(">>>" + result + " <=> " + result1);
      System.out.println();
    }
  }

  @Test public void systemTest2() throws Exception {

    final List<Document> result = Query.Result.items(Query.documents(targets, words), Document.class);
    System.out.println("finding classes sharing the typical words: " + stopwatch);

    final Grouping.Groups groups = Grouping.groupDocs(result);
    System.out.println("clustering classes: " + stopwatch);

    assertNotNull(result);
    assertNotNull(groups);

    for(Grouping.Group eachG : groups){
      System.out.print(eachG);
    }

    System.out.println();
    System.out.println("printing classes: " + stopwatch);
    stopwatch.stop();
  }


  private static Set<Source> allSourceFiles() {
    final List<Document> documents = Docs.documents();

    return documents.stream()
      .map(UsecaseTest::code)
      .collect(Collectors.toSet());
  }

  private static Source code(Document document){
    final String namespace = document.namespace();
    final String classname = document.shortName();

    return Codebase.createCode(classname,
      "package " + namespace + ";",
      "class " + classname + " {",
      "}"
    );

  }

  @AfterClass public static void tearDown() throws Exception {
    words.clear();
    words = null;

    stopWords.clear();
    stopWords = null;
  }


}
