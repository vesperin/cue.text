package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
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

  @Test public void testStringExtraction() throws Exception {

    final Corpus<String> corpus = Corpus.ofStrings();
    corpus.add("dd");
    corpus.add("Util");
    corpus.add("SetRelationDefinition");
    corpus.add("OBOButtonPanel$2");
    corpus.add("HrEditor");
    corpus.add("AOMaster$NotTerminateFilter");
    corpus.add("TypeInfoImpl");
    corpus.add("WalkingIteratorSorted");
    corpus.add("XThis$Handler");
    corpus.add("ReplacingHistogram$2");
    corpus.add("DefaultWorkingCopyOwner");


    final Selection<String> selection = new WordDistilling<>();
    final List<Word> words = selection.topKWords(3, corpus, Tokenizers.tokenizeString());
    assertThat(words.isEmpty(), is(false));
  }


  @Test public void testBasicExtraction() throws Exception {
    final WordDistilling<Source> extractor = new WordDistilling<>();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.quickSort("QuickSort4"),
      Codebase.quickSort("QuickSort5")
    );

    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(code);

    final List<Word> words = extractor.from(corpus, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    assertThat(words.isEmpty(), is(false));

    final List<Word> relevant = extractor.frequentWords(words.size(), corpus, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
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

    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(code);

    final List<Word> words = Selection.frequentWords(
      corpus,
      Tokenizers.tokenizeTypeDeclarationName()
    );

    final List<Word> twords = Selection.typicalWords(corpus, Tokenizers.tokenizeTypeDeclarationName());
    assertThat(twords.isEmpty(), is(false));

    // empty because all words are stop words
    assertThat(words.isEmpty(), is(false));
    assertThat(words.containsAll(twords), is(true));
  }

  @Test public void testTop2Words() throws Exception {
    final WordDistilling <Source> extractor = new WordDistilling<>();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(code);

    final List<Word> words  = extractor.frequentWords(2, corpus, Tokenizers.tokenizeMethodDeclarationBody(Collections.emptySet(), StopWords.all()));
    final List<Word> words2 = extractor.frequentWords(2, corpus, Tokenizers.tokenizeMethodDeclarationName(Collections.emptySet(), StopWords.all()));
    final List<Word> words3 = extractor.frequentWords(2, corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.all()));
    assertThat(words.isEmpty(), is(false));
    assertThat(words2.isEmpty(), is(false));

    assertThat(StopWords.isStopWord(StopWords.all(), "Query"), is(true));

    assertThat(words3.isEmpty(), is(true));
  }

  @Test public void testLatentWords() throws Exception {
    final WordDistilling <Source> extractor = new WordDistilling<>();
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(code);

    final List<Word> words = extractor.topKWords(5, corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));
    final List<Word> words2 = extractor.deduplicateWordList(corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));
    System.out.println(words);
    System.out.println(words2);
    assertThat(words.isEmpty(), is(false));
  }

  @Test public void testTypicalAndRepresentativeWords() throws Exception {
    final Set<Source> code = Sets.newHashSet(
      Codebase.quickSort("QuickSort1"),
      Codebase.quickSort("QuickSort2"),
      Codebase.quickSort("QuickSort3"),
      Codebase.randomCode("Query1"),
      Codebase.randomCode("Query2"),
      Codebase.randomCode("Query3")
    );

    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(code);

    final List<Word> words  = Selection.typicalWords(corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));
    final List<Word> words2 = Selection.representativeWords(corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));

    System.out.println(words);
    System.out.println(words2);
    assertThat(words.isEmpty(), is(false));
    assertThat(words2.isEmpty(), is(true));
    assertThat(words.containsAll(words2), is(true));

  }
}
