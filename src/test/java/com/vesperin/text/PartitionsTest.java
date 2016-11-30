package com.vesperin.text;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsTokenizer;
import com.vesperin.text.utils.Node;
import com.vesperin.text.utils.Tree;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Huascar Sanchez
 */
public class PartitionsTest {
  private static List<Corpus<Source>> corpusList;
  private static WordsTokenizer       tokenizer;

  @BeforeClass public static void setup() throws Exception {
    final Set<Source> code1 = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3")
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

    corpusList = Lists.newArrayList();
    corpusList.add(c1);
    corpusList.add(c2);
    corpusList.add(c3);

    tokenizer = Tokenizers.tokenizeTypeDeclarationName();
  }

  @Test public void testPartitioning() throws Exception {

    final List<Project<Source>> P = Lists.newArrayList();
    int row = 0;

    for(Corpus<Source> each : corpusList){
      final String name = ("p" + row++);
      P.add(Project.createProject(name, each, tokenizer));
    }

    final Tree<Partition<Source>> ct = Partitions.buildClusterTree(P);
    final List<Partition<Source>> pt = ct.getPreOrderTraversal().stream().map(Node::getData).collect(Collectors.toList());

    assertFalse(pt.isEmpty());

    for(Partition<Source> each : pt){
      assertNotNull(each);
      System.out.println(each + " " + each.wordSet());
    }

  }

  @AfterClass public static void tearDown() throws Exception {
    corpusList.clear();
    corpusList = null;
    tokenizer  = null;
  }
}
