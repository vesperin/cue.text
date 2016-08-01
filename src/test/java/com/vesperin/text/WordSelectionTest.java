package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Word;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class WordSelectionTest {

  @Test public void testBasicExtraction() throws Exception {
    final WordDistilling extractor = new WordDistilling();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.quickSort("QuickSort4"),
      Codebase.quickSort("QuickSort5")
    );

    final List<Word> words = extractor.from(code);
    assertThat(words.isEmpty(), is(false));

    final List<Word> relevant = extractor.frequentWords(words.size(), code);

    for(Word each : relevant){
      System.out.println(each.container() + "#" + each.element());
    }

  }

  @Test public void testNParallelExtractions() throws Exception {
    final WordDistilling extractor = new WordDistilling();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.quickSort("QuickSort4"),
      Codebase.quickSort("QuickSort5")
    );

    final List<Word> words = extractor.from(code);
    assertThat(words.isEmpty(), is(false));
  }

  @Test public void testTop2Words() throws Exception {
    final WordDistilling extractor = new WordDistilling();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final List<Word> words = extractor.frequentWords(2, code);
    assertThat(words.isEmpty(), is(false));
  }

  @Test public void testLatentWords() throws Exception {
    final WordDistilling extractor = new WordDistilling();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final List<Word> words = extractor.weightedWords(5, code);
    assertThat(words.isEmpty(), is(false));

    final Index index = Index.createIndex(words);
    index.createLsiMatrix();
  }
}
