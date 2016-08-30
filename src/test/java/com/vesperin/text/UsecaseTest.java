package com.vesperin.text;

import com.vesperin.base.Source;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class UsecaseTest {
  @Test public void test() throws Exception {
    final Set<StopWords> stopWords = StopWords.all();
    final Set<Source> sources = allSourceFiles();
    final List<Word>  words   = Selection.selects(50, sources, Selection.inspectClassName(stopWords));

    final Grouping.Groups groups = Grouping.formDocGroups(words);
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


}
