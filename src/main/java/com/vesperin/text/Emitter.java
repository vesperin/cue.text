package com.vesperin.text;

import com.vesperin.base.Context;
import com.vesperin.base.utils.Jdt;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.tokenizers.Tokenizers;
import com.vesperin.text.tokenizers.WordsTokenizer;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Emitter {
  private Emitter(){}

  /**
   * Generates a list of word pack objects from the source files
   * in a directory.
   *
   * @param fromDirectory the path to a directory
   * @return a list of word packs.
   */
  public static List<WordPack> emitWordPacks(Path fromDirectory){
    return emitWordPacks(Importer.importContexts(fromDirectory));
  }

  /**
   * Generates a list of word pack objects from a list of parsable
   * Java source files (i.e., contexts).
   *
   * @param contexts Java contexts
   * @return a list of word packs.
   */
  public static List<WordPack> emitWordPacks(List<Context> contexts){
    final List<WordPack> entries = new CopyOnWriteArrayList<>();
    final Collection<Callable<List<WordPack>>> tasks = new ArrayList<>();
    contexts.forEach(c -> tasks.add(() -> makeWordsEntry(c)));

    final Executable environment = new Executable(){};
    final ExecutorService service = environment.scaleExecutor(contexts.size());
    try {
      final List<Future<List<WordPack>>> results =  service.invokeAll(tasks);
      for(Future<List<WordPack>> each : results){
        entries.addAll(each.get());
      }
    } catch (InterruptedException | ExecutionException e){
      Thread.currentThread().interrupt();
    }

    environment.shutdownService(service);
    return entries;
  }

  private static List<WordPack> makeWordsEntry(Context context){
    final List<TypeDeclaration> typeUnits = Jdt.getTypeDeclarations(context);
    if(typeUnits.isEmpty()) {
      System.out.println("No class found. Skipping file " + context.getSource().getName());
      return new ArrayList<>();
    }

    return Collections.singletonList(makeWordsEntry(typeUnits));
  }

  private static WordPack makeWordsEntry(List<TypeDeclaration> typeUnits){
    final TypeDeclaration unit = typeUnits.get(0);
    final ITypeBinding unitBinding = unit.resolveBinding();
    final String className = unitBinding.getName();
    final Corpus<String> corpus = Corpus.ofStrings();
    corpus.add(className);

    final WordsTokenizer tokenizer = Tokenizers.tokenizeString();

    final List<Entry> words = toEntryList(freqWords(corpus, tokenizer));

    final List<WordPack> children = new ArrayList<>();
    for(MethodDeclaration eachMethod : unit.getMethods()){
      final IMethodBinding methodBinding = eachMethod.resolveBinding();
      if(methodBinding == null) continue;

      final Corpus<String> methodCorpus = Corpus.ofStrings();
      final String methodName = methodBinding.getName();
      methodCorpus.add(methodName);

      final List<Entry> methodNameFreqWords = toEntryList(freqWords(methodCorpus, tokenizer));
      final WordPack methodWordPack = new WordPack(methodName, methodNameFreqWords);
      children.add(methodWordPack);
    }


    return new WordPack(className, words, children);
  }

  /**
   * Generate a list of frequent words inside a directory
   * specified by the path object.
   *
   * @param inPath the directory to search
   * @param tokenizer the word tokenizer
   * @return a list of frequent words
   */
  public static List<Word> freqWords(Path inPath, WordsTokenizer tokenizer){
    return freqWords(Importer.makeCorpus(inPath), tokenizer);
  }

  /**
   * Generates a list of the most frequent words in a corpus.
   *
   * @param from corpus object
   * @param tokenizer strategy for collecting words in the given corpus
   * @param <T> type elements contained in the corpus.
   * @return a new list of frequent words
   */
  public static <T> List<Word> freqWords(Corpus<T> from, WordsTokenizer tokenizer){

    List<Word> words = Selection.topKFrequentWords(
            Integer.MAX_VALUE, from, tokenizer
    );

    // sorts frequent words on ascending order (from hi to lo),
    // by their frequency.
    words = words.stream()
            .sorted((a, b) -> b.count() - a.count())
            .collect(Collectors.toList());

    return words;
  }


  private static List<Entry> toEntryList(List<Word> words){
    return words.stream()
            .map(w -> new Entry(w.element(), w.count()))
            .collect(Collectors.toList());
  }


  /**
   * A word pack is a container of words for specific Java code elements, such as
   * a class, or a method. Each word pack has the following form:
   *    {name: classname, words: ['a', ...], children: [{name: methodname, words:['a', ...]}]}
   */
  public static class WordPack {
    final String name;
    final List<Entry> words;
    final List<WordPack> children;

    WordPack(String name, List<Entry> words){
      this(name, words, new ArrayList<>());
    }

    WordPack(String name, List<Entry> words, List<WordPack> children){
      this.name = name;
      this.words = words;
      this.children = children;
    }

    public String name(){
      return name;
    }

    public List<Entry> wordList(){
      return words;
    }

    public List<WordPack> children(){
      return children;
    }
  }

  static class Entry {
    final String word;
    final int count;

    Entry(String word, int count){
      this.word = word;
      this.count = count;
    }

    public String word(){
      return word;
    }

    public int count(){
      return count;
    }
  }


}
