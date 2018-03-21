package com.vesperin.text.tokenizers;

import com.vesperin.base.locations.Location;
import com.vesperin.base.locations.Locations;
import com.vesperin.base.utils.Jdt;
import com.vesperin.text.spelling.StopWords;
import com.vesperin.text.utils.Strings;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
class WordsInMethodNameTokenizer extends WordsInASTNodeTokenizer {
  WordsInMethodNameTokenizer(Set<String> whiteSet, Set<StopWords> stopWords) {
    super(whiteSet, stopWords);
  }

  private void tokenizeMethodName(ASTNode node){
    if(Objects.isNull(node)) return;
    if(ASTNode.METHOD_DECLARATION != node.getNodeType()) return;

    final MethodDeclaration declaration   = (MethodDeclaration) node;

    if(declaration.isConstructor()) return;

    final IMethodBinding    methodBinding = declaration.resolveBinding();

    // Ignore those method declarations that cannot be resolved
    if(Objects.isNull(methodBinding)) return;

    String identifier = methodBinding.getName();
    if(skipNonWhiteListed(identifier)) return;

    identifier = Strings.trimSideNumbers(identifier, false);
    final String container = resolveClassWithMethodContainer(methodBinding, identifier);

    if (visited(container)) return;

    tokenize(identifier, container);
    visit(container);

    final Location location = Locations.locate(Jdt.from(declaration), declaration);
    final int start = location.getStart().getLine() + 1;
    final int end   = location.getEnd().getLine() + 1;

    locate(container, start, end);

  }

  @Override void tokenize(ASTNode node) {
    tokenizeMethodName(node);
  }

  @Override public boolean visit(MethodDeclaration methodDeclaration) {

    tokenize(methodDeclaration);

    return false;
  }
}
