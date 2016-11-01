package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.spelling.StopWords;
import org.junit.Test;

import java.util.Collections;
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

    final List<Word> words = extractor.from(code, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    assertThat(words.isEmpty(), is(false));

    final List<Word> relevant = extractor.frequentWords(words.size(), code, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    relevant.forEach(System.out::println);

  }

  @Test public void testNParallelExtractions() throws Exception {
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.quickSort("QuickSort4"),
      Codebase.quickSort("QuickSort5")
    );

    final List<Word> words = Selection.selects(
      code,
      Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(),
        StopWords.all())
    );

    // empty because all words are stop words
    assertThat(words.isEmpty(), is(true));
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

    final List<Word> words = extractor.frequentWords(2, code, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    final List<Word> words2 = extractor.frequentWords(2, code, Tokenizers.tokenizeMethodDeclarationName(Collections.emptySet(), StopWords.all()));
    final List<Word> words3 = extractor.frequentWords(2, code, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.all()));
    assertThat(words.isEmpty(), is(false));
    assertThat(words2.isEmpty(), is(false));

    assertThat(StopWords.isStopWord(StopWords.all(), "Query"), is(true));

    assertThat(words3.isEmpty(), is(true));
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

    final List<Word> words = extractor.weightedWords(5, code, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));
    final List<Word> words2 = extractor.flattenWordList(code, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));
    System.out.println(words);
    System.out.println(words2);
    assertThat(words.isEmpty(), is(false));
  }
}
