package com.vesperin.text;

import com.google.common.collect.Iterables;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsTokenizer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class RecommendTests {
  private static Corpus<String> corpus;
  private static WordsTokenizer tokenizer;

  @BeforeClass public static void setUp() throws Exception {
    corpus = Corpus.ofStrings();
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

    tokenizer = Tokenizers.tokenizeString(StopWords.all());
  }


  @Test public void testBasicRecommend() throws Exception {

    final List<Word> words = Selection.typicalWords(corpus, tokenizer);
    assertThat(words.isEmpty(), is(false));


    final Set<String> terms     = words.stream().map(Word::element).collect(Collectors.toSet());
    final Set<String> universe  = corpus.dataSet();

    final Map<String, List<String>> map = Recommend.mappingOfLabels(terms, universe);
    assertNotNull(map);

    System.out.println(map);

  }

  @Test public void testWeirdCase() throws Exception {
    final Corpus<String> corpus = Corpus.ofStrings();
    corpus.add("selection");
    corpus.add("scope");
    corpus.add("selectedArea");
    corpus.add("errorLocation");

    final Map<List<Word>, List<Word>> maps = Introspector.generateRelevantMapping(
      corpus, Tokenizers.tokenizeString()
    );

    assertThat(maps.isEmpty(), is(false));

  }


  @Test public void testRecommendLabels() throws Exception {
    final Corpus<String> localCorpus = Corpus.ofStrings();
    localCorpus.add("range");
    localCorpus.add("saturation");
    localCorpus.add("luminance");
    localCorpus.add("yRange");
    localCorpus.add("cbRange");
    localCorpus.add("crRange");
    localCorpus.add("saturation");
    localCorpus.add("luminance");
    localCorpus.add("inLuminance");
    localCorpus.add("inSaturation");
    localCorpus.add("outLuminance");
    localCorpus.add("outSaturation");
    localCorpus.add("yRange");
    localCorpus.add("cbRange");
    localCorpus.add("crRange");

    final WordsTokenizer localTokenizer = Tokenizers.tokenizeString();

    final Map<List<Word>, List<Word>> maps = Introspector.generateRelevantMapping(
      localCorpus, localTokenizer
    );

    assertTrue(!maps.isEmpty());


    final List<Word> a = Iterables.get(maps.keySet(), 0);
    final List<Word> b = Iterables.get(maps.values(), 0);

    System.out.println(a);
    System.out.println(b);

    final List<Word> 	wordList	= b.isEmpty() ? a/*frequent words*/ : b/*typical words*/;

    assertFalse(wordList.isEmpty());

    final Set<String>	relevant	= wordList.stream().map(Word::element).collect(Collectors.toSet());
    final Set<String> universe	= localCorpus.dataSet();

    final Map<String, List<String>> wordFieldsMap = Recommend.mappingOfLabels(relevant, universe);
    assertThat(wordFieldsMap.size(), is(3));

  }

  @AfterClass public static void tearDown() throws Exception {
    corpus.clear();
    corpus    = null;
    tokenizer = null;
  }
}
