package com.vesperin.text;

import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.selection.Tokenizers;
import com.vesperin.text.spelling.StopWords;
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

  private static List<Word> words;
  private static Set<StopWords> stopWords;

  @BeforeClass public static void setup() throws Exception {
    stopWords = StopWords.all();
    final Set<Source> sources = allSourceFiles();
    words   = Selection.selects(
      50,
      sources,
      Tokenizers.tokenizeTypeDeclarationName(stopWords)
    );
  }

  @Test public void systemTest0() throws Exception {

    final Grouping.Groups groups = Grouping.reformDocGroups(words);
    assertNotNull(groups);
  }

  @Test public void systemTest1() throws Exception {

    final Map<Grouping.Group, Index> mapping = Grouping.groupIndexMapping(words);
    final Grouping.Groups groups = Grouping.reformDocGroups(mapping);

    final Index index = groups.index();

    for(Grouping.Group each : groups){
      final List<Document> documents = Grouping.Group.items(each, Document.class);
      final Query.Result result  = Query.types(documents, index);

      final Query.Result result1 = Query.labels(documents, stopWords);

      System.out.println(Document.names(documents));
      System.out.println(">>>" + result + " <=> " + result1);
      System.out.println();
    }
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
