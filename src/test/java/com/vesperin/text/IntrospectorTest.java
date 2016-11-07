package com.vesperin.text;

import com.vesperin.base.Source;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class IntrospectorTest {
  @Test public void testPartitioning() throws Exception {
    final Set<Source> sources = allSourceFiles();

    Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(sources);


    final Grouping.Groups groups = Introspector.partitionCorpus(
      corpus, Tokenizers.tokenizeTypeDeclarationName(StopWords.all())
    );

    assertThat(groups.isEmpty(), is(false));
  }

  @Test public void testTypicality() throws Exception {
    final Set<Source> sources = allSourceFiles();

    Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(sources);

    final List<Word> typicalOnes = Introspector.typicalWords(
      corpus, Tokenizers.tokenizeTypeDeclarationName(StopWords.all())
    );

    assertThat(typicalOnes.isEmpty(), is(false));
  }

  @Test public void testCorpusRepresentativeness() throws Exception {
    final Set<Source> sources = allSourceFiles();

    Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(sources);

    final List<Word> representativeWords = Introspector.representativeWords(
      corpus, Tokenizers.tokenizeTypeDeclarationName(StopWords.all())
    );

    assertThat(representativeWords.isEmpty(), is(false));
  }


  @Test public void testCorpusRepresentativeness2() throws Exception {
    final Set<Source> sources = allSourceFiles();

    Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(sources);

    final Map<List<Word>, List<Word>> relevantMapping = Introspector.generateRelevantMapping(
      corpus, Tokenizers.tokenizeTypeDeclarationName(StopWords.all())
    );

    final List<Word> representativeWords = Introspector.representativeWords(relevantMapping);

    assertThat(representativeWords.isEmpty(), is(false));
  }

  private static Set<Source> allSourceFiles() {
    final List<Selection.Document> documents = Docs.documents();

    return documents.stream()
      .map(IntrospectorTest::code)
      .collect(Collectors.toSet());
  }

  private static Source code(Selection.Document document){
    final String namespace = document.namespace();
    final String classname = document.shortName();

    return Codebase.createCode(classname,
      "package " + namespace + ";",
      "class " + classname + " {",
      "}"
    );

  }
}
