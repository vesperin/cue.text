package com.vesperin.text;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Query.Result;
import com.vesperin.text.Selection.Document;
import com.vesperin.text.Selection.Word;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class QueryTest {
  private static Set<Source> code;

  @BeforeClass public static void setup(){
    code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );
  }

  @Test public void testSearching() throws Exception {

    final List<Word>  words  = Selection.selects(100, code);
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

    final List<Word>  words  = Selection.selects(100, code);
    final Groups      groups = Grouping.formDocGroups(words);
    final Index       index  = groups.index();

    System.out.println(groups.groupList().size());

    assertTrue(!groups.isEmpty());

    final Group       group  = Iterables.get(groups, 0/*most typical*/);

    final List<Document>  keywords = Group.items(group, Document.class);

    for(Document each : keywords){
      final Result result = Query.types(Collections.singletonList(each), index);
      assertNotNull(result);

      System.out.println(each + ": " + result);
    }
  }

  @AfterClass public static void tearDown(){
    code.clear();
    code = null;
  }
}
