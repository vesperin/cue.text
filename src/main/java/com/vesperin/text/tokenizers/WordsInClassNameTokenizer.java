package com.vesperin.text.tokenizers;

import com.vesperin.text.spelling.StopWords;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.Objects;
import java.util.Set;

/**
 * @author Huascar Sanchez
 */
class WordsInClassNameTokenizer extends WordsInASTNodeTokenizer {

  WordsInClassNameTokenizer(Set<String> whiteSet, Set<StopWords> stopWords) {
    super(whiteSet, stopWords);
  }

  @Override void tokenize(ASTNode node) {
    if (ASTNode.TYPE_DECLARATION != node.getNodeType()) return;

    final TypeDeclaration declaration = (TypeDeclaration) node;

    final SimpleName  name          = declaration.getName();
    final String      subclassName  = name.getIdentifier();
    final String      container     = resolveContainer(name);

    if (visited(container)) return;

    if (!isValid(subclassName)) {
      visit(container);
      return;
    }


    final ITypeBinding typeBinding = declaration.resolveBinding();
    // Ignore those type declarations that have unresolved bindings.
    if(Objects.isNull(typeBinding)) return;

    final ITypeBinding superClass = typeBinding.getSuperclass();
    if(!Objects.isNull(superClass) && !superClass.getName().equals("Object")){

      final String superclassName = superClass.getErasure().getName();

      tokenize(subclassName, superclassName, container);
      visit(container);

    } else {

      final ITypeBinding[] interfaces = typeBinding.getInterfaces();
      if(interfaces.length > 0){

        // we assume that the first interface is
        // the most interesting one.
        final ITypeBinding first = interfaces[0];
        final String superInterfaceName = first.getErasure().getName();

        tokenize(subclassName, superInterfaceName, container);
        visit(container);

      } else {
        tokenize(subclassName, container);
        visit(container);
      }

    }

  }

  @Override public boolean visit(TypeDeclaration typeDeclaration) {
    if (!typeDeclaration.isPackageMemberTypeDeclaration()) {
      final ITypeBinding binding = typeDeclaration.resolveBinding();
      System.out.println(
              "I: {\"" + binding.getName() + "\": \""
                      + typeDeclaration.resolveBinding().getQualifiedName() + "\"},"
      );
      return false;
    }

    tokenize(typeDeclaration);

    return true;
  }
}
