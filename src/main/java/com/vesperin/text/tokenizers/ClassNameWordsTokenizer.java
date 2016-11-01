package com.vesperin.text.tokenizers;

import com.vesperin.text.spelling.StopWords;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Set;

/**
 * @author Huascar Sanchez
 */
class ClassNameWordsTokenizer extends ASTNodeWordsTokenizer {

  public ClassNameWordsTokenizer(Set<String> whiteSet, Set<StopWords> stopWords) {
    super(whiteSet, stopWords);
  }

  @Override void tokenize(ASTNode node) {
    if (ASTNode.TYPE_DECLARATION != node.getNodeType()) return;

    final TypeDeclaration declaration = (TypeDeclaration) node;

    final SimpleName  name        = declaration.getName();
    final String      identifier  = name.getIdentifier();
    final String      container   = resolveContainer(name);

    if (visited(container)) return;

    if (!isValid(identifier)) {
      visit(container);
      return;
    }

    tokenize(identifier, container);
    visit(container);

  }

  @Override public boolean visit(TypeDeclaration typeDeclaration) {
    if (!typeDeclaration.isPackageMemberTypeDeclaration()) return true;

    tokenize(typeDeclaration);

    return false;
  }
}
