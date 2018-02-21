package com.vesperin.text;

import com.google.common.collect.Sets;
import com.vesperin.base.Source;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.utils.Strings;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class WordSelectionTest {

  // todo RESTORE TO ORIGINAL
  @Test public void testStringExtraction() throws Exception {

    final Corpus<String> corpus = Corpus.ofStrings();
//    corpus.add("dd");
//    corpus.add("Util");
//    corpus.add("SetRelationDefinition");
//    corpus.add("OBOButtonPanel$2");
//    corpus.add("HrEditor");
//    corpus.add("AOMaster$NotTerminateFilter");
//    corpus.add("TypeInfoImpl");
//    corpus.add("WalkingIteratorSorted");
//    corpus.add("XThis$Handler");
//    corpus.add("ReplacingHistogram$2");
//    corpus.add("DefaultWorkingCopyOwner");
    corpus.add("angularVelocity");
    corpus.add("linearVelocity");
    corpus.add("externalForce");
    corpus.add("externalTorque");
    corpus.add("previousSeparatingAxis");
    corpus.add("closestPoint");
    corpus.add("direction");
    corpus.add("minkDiff");
    corpus.add("pointA");
    corpus.add("pointB");
    corpus.add("relativeDirection");
    corpus.add("supportA");
    corpus.add("supportB");
    corpus.add("mMaxCoordinates");
    corpus.add("mMinCoordinates");
    corpus.add("extent");
    corpus.add("maxBounds");
    corpus.add("minBounds");
    corpus.add("mLocalAnchorPointBody1");
    corpus.add("mLocalAnchorPointBody2");
    corpus.add("mR1World");
    corpus.add("mR2World");
    corpus.add("mBiasVector");
    corpus.add("mImpulse");
    corpus.add("anchorPointWorldSpace");
    corpus.add("localPointOnBody1");
    corpus.add("localPointOnBody2");
    corpus.add("normal");
    corpus.add("worldPointOnBody1");
    corpus.add("worldPointOnBody2");
    corpus.add("normal");
    corpus.add("localPoint1");
    corpus.add("localPoint2");
    corpus.add("mLocalAnchorPointBody1");
    corpus.add("mLocalAnchorPointBody2");
    corpus.add("mR1World");
    corpus.add("mR2World");
    corpus.add("mImpulseTranslation");
    corpus.add("mImpulseRotation");
    corpus.add("mBiasTranslation");
    corpus.add("mBiasRotation");
    corpus.add("anchorPointWorldSpace");
    corpus.add("mLocalAnchorPointBody1");
    corpus.add("mLocalAnchorPointBody2");
    corpus.add("mHingeLocalAxisBody1");
    corpus.add("mHingeLocalAxisBody2");
    corpus.add("mA1");
    corpus.add("mR1World");
    corpus.add("mR2World");
    corpus.add("mB2CrossA1");
    corpus.add("mC2CrossA1");
    corpus.add("mImpulseTranslation");
    corpus.add("mBTranslation");
    corpus.add("anchorPointWorldSpace");
    corpus.add("rotationAxisWorld");
    corpus.add("mLocalAnchorPointBody1");
    corpus.add("mLocalAnchorPointBody2");
    corpus.add("mSliderAxisBody1");
    corpus.add("mN1");
    corpus.add("mN2");
    corpus.add("mR1");
    corpus.add("mR2");
    corpus.add("mR2CrossN1");
    corpus.add("mR2CrossN2");
    corpus.add("mR2CrossSliderAxis");
    corpus.add("mR1PlusUCrossN1");
    corpus.add("mR1PlusUCrossN2");
    corpus.add("mR1PlusUCrossSliderAxis");
    corpus.add("mBRotation");
    corpus.add("mImpulseRotation");
    corpus.add("mSliderAxisWorld");
    corpus.add("anchorPointWorldSpace");
    corpus.add("sliderAxisWorldSpace");
    corpus.add("frictionVector1");
    corpus.add("frictionVector2");
    corpus.add("frictionPointBody1");
    corpus.add("frictionPointBody2");
    corpus.add("frictionVector1");
    corpus.add("frictionVector2");
    corpus.add("normal");
    corpus.add("oldFrictionVector1");
    corpus.add("oldFrictionVector2");
    corpus.add("r1CrossT1");
    corpus.add("r1CrossT2");
    corpus.add("r2CrossT1");
    corpus.add("r2CrossT2");
    corpus.add("r1Friction");
    corpus.add("r2Friction");
    corpus.add("frictionVector1");
    corpus.add("frictionVector2");
    corpus.add("normal");
    corpus.add("oldFrictionVector1");
    corpus.add("oldFrictionVector2");
    corpus.add("r1");
    corpus.add("r2");
    corpus.add("r1CrossN");
    corpus.add("r2CrossN");
    corpus.add("r1CrossT1");
    corpus.add("r1CrossT2");
    corpus.add("r2CrossT1");
    corpus.add("r2CrossT2");
    corpus.add("gravity");
    corpus.add("angularImpulseBody1");
    corpus.add("angularImpulseBody2");
    corpus.add("linearImpulseBody1");
    corpus.add("linearImpulseBody2");
    corpus.add("cachedSeparatingAxis");
    corpus.add("position");


    final Selection<String> selection = new WordDistilling<>();
    final List<Word> wordList = selection.from(corpus, Tokenizers.tokenizeString(StopWords.all()));
    final List<Word> filtered = selection.from(wordList, new Selection.WordByCompositeWeight());

    System.out.println("All words");
    System.out.println(wordList);
    System.out.println(filtered);
    System.out.println("WordList ^ Filtered");
    final Set<Word> intersecting = Sets.intersection(wordList.stream().collect(Collectors.toSet()), filtered.stream().collect(Collectors.toSet()));
    System.out.println(intersecting);

    final List<Word> words = Introspector.frequentWords(corpus, Tokenizers.tokenizeString(StopWords.all()));
    System.out.println("Auto filtered words");
    System.out.println(words);
    assertThat(words.isEmpty(), is(false));


    final Set<String> terms     = words.stream().map(Word::element).collect(Collectors.toSet());
    final Set<String> universe  = corpus.dataSet();

    final Map<String, List<String>> map = Strings.generateCoverageRegion(terms, universe);
    System.out.println(map);

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

    final List<Word> words = Introspector.frequentWords(
      corpus,
      Tokenizers.tokenizeTypeDeclarationName()
    );

    final List<Word> twords = Introspector.typicalityQuery(100, corpus, Tokenizers.tokenizeTypeDeclarationName());
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

    final List<Word> words  = Introspector.typicalityQuery(200, corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));
    final List<Word> words2 = Introspector.representativeWords(corpus, Tokenizers.tokenizeTypeDeclarationName(Collections.emptySet(), StopWords.of(StopWords.JAVA, StopWords.ENGLISH)));

    System.out.println(words);
    System.out.println(words2);
    assertThat(words.isEmpty(), is(false));
    assertThat(words2.isEmpty(), is(false));
    assertThat(words.containsAll(words2), is(true));

  }
}
