package com.vesperin.text;

import com.vesperin.base.Context;
import com.vesperin.base.Source;
import com.vesperin.text.utils.Ios;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Importer {
  /**
   * Collects all source files in a given directory.
   *
   * @param directory location of source files.
   * @return the list source objects.
   */
  public static List<Source> importSources(Path directory) {
    if (directory == null) return new ArrayList<>();
    if (!Files.exists(directory)) return new ArrayList<>();
    final List<File> files = Ios.collectFiles(directory, "java");
    return importJavafiles(files);
  }

  /**
   * Converts a list of files into a list of source objects.
   *
   * @param files the files to be converted
   * @return the list source objects.
   */
  public static List<Source> importJavafiles(List<File> files) {
    final Predicate<Source> noPackageInfoFiles =
            s -> !"package-info".equals(s.getName());

    return files.parallelStream()
            .map(Importer::importSource)
            .filter(noPackageInfoFiles)
            .collect(Collectors.toList());
  }

  /**
   * Converts file into a Source object.
   *
   * @param file the file to be converted
   * @return the Source object.
   */
  public static Source importSource(File file) {
    final String name = fileNameWithoutExt(file);
    final String content = Ios.readLines(file.toPath()).stream()
            .collect(Collectors.joining("\n"));

    return Source.from(name, content);
  }

  private static String fileNameWithoutExt(File file) {
    String name = file.getName();
    final int pos = name.lastIndexOf(".");

    if (pos > 0) {
      name = name.substring(0, pos);
    }

    return name;
  }


  /**
   * Generates a corpus object from a list of files in a directory.
   *
   * @param directory path to source files
   * @return a corpus of source objects.
   */
  public static Corpus<Source> makeCorpus(Path directory){
    final Corpus<Source> corpus = Corpus.ofSources();
    corpus.addAll(importSources(directory));
    return corpus;
  }

  /**
   * Generates list of contexts from a corpus object.
   *
   * @param corpus a corpus object.
   * @return a list of context objects
   */
  public static List<Context> makeContextList(Corpus<Source> corpus){
    if (corpus == null) return new ArrayList<>();
    if (corpus.size() == 0) return new ArrayList<>();

    final List<Context> contexts = new ArrayList<>();
    for (Source each : corpus){
      final Context context = Selection.newContext(each);
      if (context == null) continue;
      contexts.add(context);
    }

    return contexts;
  }

  /**
   * Parses a list of Java files found in some directory and
   * then returns a list of parsed Java files (i.e., context objects).
   *
   * @param from the directory from where files are retrieved.
   * @return a new list of context objects.
   */
  public static List<Context> importContexts(Path from){
    final Corpus<Source> corpus = makeCorpus(from);
    return makeContextList(corpus);
  }

}
