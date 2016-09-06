package com.vesperin.text;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Query.Result;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class QueryTest {
  private static Set<Source> code;
  private static List<Document> documents;

  @BeforeClass public static void setup(){
    code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    documents = Docs.documents();
  }

  @Test public void testSearching() throws Exception {

    final List<Word>  words  = Selection.selects(100, code, Selection.inspectMethodBody(Collections.emptySet(), StopWords.all()));
    final Groups      groups = Grouping.formWordGroups(words);
    final Index       index  = groups.index();

    assertTrue(!groups.isEmpty());

    final Group       group  = Iterables.get(groups, 0/*most typical*/);

    final List<Word>  keywords = Group.items(group, Word.class);
    for(Word each : keywords){
      final Result result = Query.methods(Collections.singletonList(each), index);
      assertNotNull(result);

      System.out.println(each + ": " + result);
    }


  }

  @Test public void testTypeSearching() throws Exception {

    final List<Word>  words   = Selection.selects(100, code, Selection.inspectClassName(StopWords.of(StopWords.JAVA)));
    final Groups      groups  = Grouping.formDocGroups(words);

    final Map<Group, Index> mapping = Grouping.groupIndexMapping(words);
    final Groups      groups1 = Grouping.reformDocGroups(mapping);


    System.out.println("Printing group formation");
    System.out.println(groups);

    System.out.println("Printing group reformation");
    System.out.println(groups1);

    final Index       index  = groups.index();

    System.out.println(groups.groupList().size());

    assertTrue(!groups.isEmpty());

    final Group       group  = Iterables.get(groups, 0/*most typical*/);
    final List<Document>  keywords = Group.items(group, Document.class);

    for(Document each : keywords){
      final Result result = Query.types(Collections.singletonList(each), index);
      assertNotNull(result);
    }
  }

  @Test public void testReGrouping() throws Exception {

    final List<Word>  words   = Selection.selects(100, code, Selection.inspectClassName(StopWords.of(StopWords.JAVA)));
    final Groups      groups  = Grouping.formDocGroups(words);

    final Map<Grouping.Group, Index> mapping = Grouping.groupIndexMapping(words);
    final Groups      groups1 = Grouping.reformDocGroups(mapping);

    System.out.println("Printing group reformation");
    System.out.println(groups1);

    final Index       index  = groups.index();

    System.out.println(groups.groupList().size());

    assertTrue(!groups.isEmpty());


    final Group  g1 = Group.merge(Document.class, groups);
    final Groups g2 = Grouping.formDocGroups(g1);


    assertTrue(!g2.isEmpty());


    for(Group eachGroup : g2){
      final List<Document> docs = Group.items(eachGroup, Document.class);
      final Result result = Query.types(docs, index);

      assertNotNull(result);
      assertTrue(Result.items(result, Word.class).size() < 3);

      System.out.println(Document.names(docs) + ": " + result);
    }
  }

  @Test public void testLabelExtraction() throws Exception {
    final Stopwatch start = Stopwatch.createStarted();
    Grouping.Group g = new Grouping.BasicGroup();
    documents.forEach(g::add);
    System.out.println("creating a group:" + start);

    final Grouping.Groups gp = Grouping.formDocGroups(g, 36);

    System.out.println("forming groups of groups:" + start);
    for(Grouping.Group each : gp){
      final List<Document> ds = Group.items(each, Document.class);
      final Result result = Query.labels(ds, StopWords.all());

      final List<String> names     = Document.names(ds);
      System.out.println(names + " " + result);
    }
  }

  @AfterClass public static void tearDown(){
    code.clear();
    code = null;
    documents.clear();
    documents = null;
  }
}
