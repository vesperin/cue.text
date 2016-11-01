package com.vesperin.text.selection;

import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Strings;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.Locale;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
class MethodNameWordsTokenizer extends ASTNodeWordsTokenizer {
  public MethodNameWordsTokenizer(Set<String> whiteSet, Set<StopWords> stopWords) {
    super(whiteSet, stopWords);
  }

  @Override void tokenize(ASTNode node) {

    if(ASTNode.METHOD_DECLARATION != node.getNodeType()) return;

    final MethodDeclaration declaration = (MethodDeclaration) node;
    final SimpleName        name        = declaration.getName();

    if(skipNonWhiteListed(name.getIdentifier())) return;

    final String identifier = Strings.trimSideNumbers(name.getIdentifier(), false);

    if (visited(identifier)) return;

    if (!isValid(identifier)) {
      visit(identifier);
      return;
    }

    final String container = resolveContainer(name);

    tokenize(identifier, container);
    visit(container);

  }

  @Override public boolean visit(MethodDeclaration methodDeclaration) {

    tokenize(methodDeclaration);

    return false;
  }
}
