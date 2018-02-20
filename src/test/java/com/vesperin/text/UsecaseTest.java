package com.vesperin.text;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.utils.Ios;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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

//    final Map<List<Word>, List<Word>> mapping = Introspector.buildWordsMap(corpus, Tokenizers.tokenizeTypeDeclarationName(stopWords));
//
//    words   = Iterables.get(mapping.keySet(), 0).stream().limit(50).collect(Collectors.toList());
//
//    System.out.println("collecting top 50 frequent words: " + stopwatch);
//
//    targets = Iterables.get(mapping.values(), 0);
//
//    System.out.println("identifying the most typical words: " + stopwatch);
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

      System.out.println(Document.paths(documents));
      System.out.println(">>>" + result + " <=> " + result1);
      System.out.println();
    }
  }

  @Test public void systemTest2() throws Exception {

    final Corpus<Source> theCorpus = Corpus.ofSources();
//    theCorpus.addAll(allSourceFiles());
//    theCorpus.addAll(getCorpus("/Users/hsanchez/dev/data/corpus/jbox2d"));
    theCorpus.add(Codebase.failingCode());

    final Grouping.Groups theGroups = Grouping.groupDocs(theCorpus, Tokenizers.tokenizeMethodDeclarationName());

    final Map<String, List<Document>> clusters = new HashMap<>();
    for(Grouping.Group each : theGroups){
      final Grouping.NamedGroup namedGroup = (Grouping.NamedGroup) each;
      final List<Document> values = Grouping.Group.items(each, Document.class);

      if(!clusters.containsKey(namedGroup.toString())){
        clusters.put(namedGroup.toString(), new ArrayList<>());
      }

      clusters.get(namedGroup.toString()).addAll(values);
    }

    clusters.entrySet().forEach(e -> System.out.println(e.getKey() + ":" + e.getValue()));


    final StringBuilder entryBuilder = new StringBuilder(clusters.size() * 10);
    final Iterator<String> keys = clusters.keySet().iterator();
    while(keys.hasNext()){

      final String eachKey = keys.next();

      final List<Document> documents = clusters.get(eachKey);

      entryBuilder.append("{\n")
        .append("\t\"types\": [\n");

      final Iterator<Document> documentIterator = documents.iterator();
      while(documentIterator.hasNext()){
        final Document d = documentIterator.next();
        entryBuilder.append("\t\t\"").append(d.toString()).append("\"");
        if(documentIterator.hasNext()){
          entryBuilder.append(",\n");
        }
      }

      entryBuilder.append("\n\t],\n\t\"labels\": [\n");
      entryBuilder.append("\t\"").append(eachKey).append("\"")
        .append("\t\n\t]\n").append("}");


      if(keys.hasNext()){
        entryBuilder.append(",\n");
      }


    }


    System.out.println(entryBuilder.toString());

//    final List<Document> result = Query.Result.items(Query.documents(targets, words), Document.class);
//    System.out.println("finding classes sharing the typical words: " + stopwatch);
//
//    final Grouping.Groups groups = Grouping.groupDocs(result);
//    System.out.println("clustering classes: " + stopwatch);
//
//    assertNotNull(result);
//    assertNotNull(groups);
//
//    for(Grouping.Group eachG : groups){
//      System.out.print(eachG);
//    }
//
//    System.out.println();
//    System.out.println("printing classes: " + stopwatch);
//    stopwatch.stop();
  }


  private static Set<Source> allSourceFiles() {
    final List<Document> documents = Docs.documents();

    return documents.stream()
      .map(UsecaseTest::code)
      .collect(Collectors.toSet());
  }

  /**
   * Converts a list of files into a list of source objects.
   *
   * @param files the files to be converted
   * @return the list source objects.
   */
  public static List<Source> from(List<File> files) {
    final Predicate<Source> noPackageInfoFiles = s -> !"package-info".equals(s.getName());

    return files.stream()
      .map(UsecaseTest::from)
      .filter(noPackageInfoFiles)
      .collect(Collectors.toList());
  }

  /**
   * Converts a file into a source object.
   *
   * @param file the file to be converted.
   * @return a new source code object.
   */
  public static Source from(File file) {
    try {
      final String name     = Files.getNameWithoutExtension(file.getName());
      final String content  = Files.readLines(file, Charset.defaultCharset()).stream()
        .collect(Collectors.joining("\n"));

      return Source.from(name, content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  static Set<Source> getCorpus(String directory){
    final Corpus<Source> corpus = Corpus.ofSources();
    final Path start = Paths.get(directory);
    corpus.addAll(from(Ios.collectFiles(start, "java", "Test", "test", "package-info")));

    final Set<Source> corpusSet = corpus.dataSet().stream().collect(Collectors.toSet());
    return corpusSet;
  }

  private static Source code(Document document){
    final String namespace = document.namespace();
    final String classname = document.shortName();

    return Codebase.createCode(classname,
      "package " + namespace + ";",
      "class " + classname + " {",
        "\n public void testMe" + document.id() + "(){}\n"
          + "\n public void wowDust" + document.id() + "(){}\n"
          + "\n static class Awesome {}\n" +
      "}"
    );

  }

  @AfterClass public static void tearDown() throws Exception {
//    words.clear();
//    words = null;
//
//    stopWords.clear();
//    stopWords = null;
  }


}
