package com.vesperin.text.tokenizers;

import com.vesperin.base.utils.Jdt;
import com.vesperin.base.visitors.SkeletalVisitor;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Strings;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

/**
 * @author Huascar Sanchez
 */
class MethodBodyWordsTokenizer extends ASTNodeWordsTokenizer {
  public MethodBodyWordsTokenizer(Set<String> whiteSet, Set<StopWords> stopWords) {
    super(whiteSet, stopWords);
  }

  @Override void tokenize(ASTNode node) {

    if(ASTNode.METHOD_DECLARATION != node.getNodeType()) return;

    final MethodDeclaration declaration = (MethodDeclaration) node;
    final SimpleName        name        = declaration.getName();

    if(skipNonWhiteListed(name.getIdentifier())) return;

    final String identifier = Strings.trimSideNumbers(name.getIdentifier(), false);

    if (visited(identifier)) return;

    final Optional<Block> optionalBlock = Jdt.getChildren(declaration).stream()
      .filter(n -> ASTNode.BLOCK == n.getNodeType())
      .map(n -> (Block) n)
      .findFirst();

    final String container  = resolveContainer(declaration.getName());

    if (optionalBlock.isPresent()) {
      final Block block = optionalBlock.get();
      final SimpleNameVisitor visitor = new SimpleNameVisitor();
      block.accept(visitor);

      final List<String> ws = visitor.names
        .stream()
        .map(Strings::wordSplit)
        .flatMap(Arrays::stream)
        .collect(toList());

      final String[] splits   = ws.toArray(new String[ws.size()]);

      process(splits, container);

    }

    visit(container);

  }

  @Override public boolean visit(MethodDeclaration declaration) {

    tokenize(declaration);

    return false;
  }


  private static class SimpleNameVisitor extends SkeletalVisitor {
    final Set<String> names = new HashSet<>();
    @Override public boolean visit(SimpleName simpleName) {
      names.add(simpleName.getIdentifier());
      return false;
    }
  }
}
