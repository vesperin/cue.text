package com.vesperin.text.selection;

import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.SkeletalVisitor;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Strings;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public abstract class ASTNodeWordsTokenizer extends SkeletalVisitor implements WordsTokenizer {

  private final List<Word>      items;
  private final Set<StopWords>  stopWords;
  private final Set<String>     whiteSet;
  private final Set<String>     visited;

  /**
   * Constructs a new Word collection strategy.
   *
   * @param whiteSet allowed words
   * @param stopWords disallowed words
   */
  ASTNodeWordsTokenizer(Set<String> whiteSet, Set<StopWords> stopWords) {
    this.whiteSet = whiteSet.stream()
      .map(s -> s.toLowerCase(Locale.ENGLISH))
      .collect(Collectors.toSet());

    this.stopWords  = stopWords;
    this.items      = new ArrayList<>();
    this.visited    = new HashSet<>();
  }

  @Override public boolean isLightweightTokenizer() {
    return false;
  }

  @Override public List<Word> wordsList() {
    return items;
  }

  /**
   * Tokenize an AST node.
   *
   * @param node AST node to tokenize
   */
  abstract void tokenize(ASTNode node);

  /**
   * Tokenize some text extracted from either a given container or the body of
   * a given container.
   *
   * @param text raw text
   * @param container either a class name or a method name.
   */
  void tokenize(String text, String container){
    if (!skipThrowablesAlike(text)) {
      // make sure we have a valid split
      String[] split = Strings.wordSplit(text);

      // process all split tokens
      process(split, container);
    }
  }

  @Override public void clear() {
    synchronized (this) {
      this.visited.clear();
      this.items.clear();
    }
  }

  private static boolean skipThrowablesAlike(String identifier) {
    return (identifier.endsWith("Exception")
      || identifier.equals("Throwable")
      || identifier.equals("Error"));
  }

  boolean skipNonWhiteListed(String text){
    return (!whiteSet.contains(text.toLowerCase(Locale.ENGLISH)) && !whiteSet.isEmpty());
  }

  static boolean isValid(String identifier) {
    final String pattern = Pattern.quote("_");
    final boolean underscored = identifier.split(pattern).length == 1;
    final boolean onlyConsonants = Strings.onlyConsonantsOrVowels(identifier);
    final boolean tooSmall = identifier.length() < 4;

    return !((underscored && onlyConsonants) || tooSmall);
  }

  private static String packageName(TypeDeclaration type) {
    assert !Objects.isNull(type);
    assert !Objects.isNull(type.getRoot());

    final CompilationUnit unit = Jdt.parent(CompilationUnit.class, type.getRoot());
    final Optional<PackageDeclaration> op = Optional.ofNullable(unit.getPackage());

    String packageName = "";
    if (op.isPresent()) {
      packageName = op.get().getName().getFullyQualifiedName() + ".";
    }

    return packageName;
  }

  static String resolveContainer(SimpleName name) {
    final Optional<TypeDeclaration> type = Optional.ofNullable(Jdt.parent(TypeDeclaration.class, name));
    final Optional<MethodDeclaration> method = Optional.ofNullable(Jdt.parent(MethodDeclaration.class, name));

    String packageName = "";
    if (type.isPresent()) {
      packageName = packageName(type.get());
    }

    final String left = type.isPresent() ? (packageName + type.get().getName().getFullyQualifiedName()) + (method.isPresent() ? "#" : "") : "";
    final String right = method.isPresent() ? method.get().getName().getIdentifier() + (method.get().isConstructor() ? "(C)" : "") : "";

    return left + right;
  }

  @Override public Set<StopWords> stopWords() {
    return stopWords;
  }


  void visit(String container){
    visited.add(container);
  }


  boolean visited(String container){
    return container != null && visited.contains(container);
  }

}
