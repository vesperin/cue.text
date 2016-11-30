package com.vesperin.text.utils;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Huascar Sanchez
 */
public class TreeTest {
  @Test public void testBasic() throws Exception {
    final Node<String> root = Node.newNode("Root");
    final Tree<String> tree = new Tree<>(root);

    final Node<String> a = Node.newNode("a");
    final Node<String> b = Node.newNode("b");

    root.addChild(a);
    root.addChild(b);

    final Node<String> c = Node.newNode("c");
    final Node<String> d = Node.newNode("d");

    a.addChild(c);
    b.addChild(d);


    final Deque<Node<String>> Q = new ArrayDeque<>();
    final Set<Node<String>>   V = Sets.newIdentityHashSet();

    assertNotNull(tree.getRoot());

    Q.add(tree.getRoot());

    while(!Q.isEmpty()){

      final Node<String> w = Q.remove();
      if(!V.contains(w)){
        V.add(w);

        for(Node<String> child : w.getChildren()){
          System.out.println(child.getData());
          Q.add(child);
        }
      }
    }

    assertTrue(Q.isEmpty());
  }
}
