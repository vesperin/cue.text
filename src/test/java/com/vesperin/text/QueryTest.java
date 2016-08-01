package com.vesperin.text;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Grouping.Group;
import com.vesperin.text.Grouping.Groups;
import com.vesperin.text.Query.Result;
import com.vesperin.text.Selection.Word;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;

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
    final Groups      groups = Grouping.formGroups(words);
    final Index       index  = groups.index();
    final Group       group  = Iterables.get(groups, 0/*most typical*/);

    final List<Word>  keywords = group.wordList();

    final Result result = Query.methods(keywords, index);
    assertNotNull(result);

    System.out.println(result);

  }

  @AfterClass public static void tearDown(){
    code.clear();
    code = null;
  }
}
