package com.vesperin.text.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic n-ary tree.
 *
 * @param <T> Any class type
 */
public class Tree<T> {

  private Node<T> root;

  public Tree(){
    this(null);
  }

  /**
   * Initialize a tree with the specified root node.
   *
   * @param root The root node of the tree
   */
  public Tree(Node<T> root) {
    this.root = root;
  }

  /**
   * Checks if the tree is empty (root node is null)
   *
   * @return <code>true</code> if the tree is empty,
   * <code>false</code> otherwise.
   */
  public boolean isEmpty() {
    return root == null;
  }

  /**
   * Get the root node of the tree
   *
   * @return the root node.
   */
  public Node<T> getRoot() {
    return root;
  }

  /**
   * Set the root node of the tree. Replaces existing root node.
   *
   * @param rootData The root data to replace the existing root node with.
   */
  public void setRoot(T rootData) {
    this.setRoot(Node.newNode(rootData));
  }

  /**
   * Set the root node of the tree. Replaces existing root node.
   *
   * @param root The root node to replace the existing root node with.
   */
  public void setRoot(Node<T> root) {
    this.root = root;
  }

  /**
   *
   * Check if given data is present in the tree.
   *
   * @param key The data to search for
   * @return <code>true</code> if the given key was found in the tree,
   * <code>false</code> otherwise.
   */
  public boolean exists(T key) {
    return find(root, key);
  }

  /**
   * Get the number of nodes (size) in the tree.
   *
   * @return The number of nodes in the tree
   */
  public int size() {
    return getNumberOfDescendants(root) + 1;
  }

  /**
   *
   * Get the number of descendants a given node has.
   *
   * @param node The node whose number of descendants is needed.
   * @return the number of descendants
   */
  public int getNumberOfDescendants(Node<T> node) {
    int n = node.getChildren().size();
    for (Node<T> child : node.getChildren())
      n += getNumberOfDescendants(child);

    return n;

  }

  private boolean find(Node<T> node, T keyNode) {
    boolean res = false;
    if (node.getData().equals(keyNode))
      return true;

    else {
      for (Node<T> child : node.getChildren())
        if (find(child, keyNode))
          res = true;
    }

    return res;
  }

  private Node<T> findNode(Node<T> node, T keyNode) {
    if (node == null)
      return null;
    if (node.getData().equals(keyNode))
      return node;
    else {
      Node<T> cnode = null;
      for (Node<T> child : node.getChildren())
        if ((cnode = findNode(child, keyNode)) != null)
          return cnode;
    }
    return null;
  }

  /**
   *
   * Get the list of nodes arranged by the pre-order traversal of the tree.
   *
   * @return The list of nodes in the tree, arranged in the pre-order
   */
  public List<Node<T>> getPreOrderTraversal() {
    return getPreOrderTraversal(getRoot());
  }

  public List<Node<T>> getPreOrderTraversal(Node<T> node){
    List<Node<T>> preOrder = new ArrayList<>();
    buildPreOrder(node, preOrder);
    return preOrder;
  }

  /**
   *
   * Get the list of nodes arranged by the post-order traversal of the tree.
   *
   * @return The list of nodes in the tree, arranged in the post-order
   */
  public ArrayList<Node<T>> getPostOrderTraversal() {
    ArrayList<Node<T>> postOrder = new ArrayList<>();
    buildPostOrder(root, postOrder);
    return postOrder;
  }

  private void buildPreOrder(Node<T> node, List<Node<T>> preOrder) {
    preOrder.add(node);
    for (Node<T> child : node.getChildren()) {
      buildPreOrder(child, preOrder);
    }
  }

  private void buildPostOrder(Node<T> node, List<Node<T>> postOrder) {
    for (Node<T> child : node.getChildren()) {
      buildPostOrder(child, postOrder);
    }
    postOrder.add(node);
  }

  /**
   *
   * Get the list of nodes in the longest path from root to any leaf in the tree.
   *
   * For example, for the below tree
   * <pre>
   *          A
   *         / \
   *        B   C
   *           / \
   *          D  E
   *              \
   *              F
   * </pre>
   *
   * The result would be [A, C, E, F]
   *
   * @return The list of nodes in the longest path.
   */
  public List<Node<T>> getLongestPathFromRootToAnyLeaf() {
    List<Node<T>> longestPath = null;
    int max = 0;
    for (List<Node<T>> path : getPathsFromRootToAnyLeaf()) {
      if (path.size() > max) {
        max = path.size();
        longestPath = path;
      }
    }
    return longestPath;
  }


  /**
   *
   * Get the height of the tree (the number of nodes in the longest path from root to a leaf)
   *
   * @return The height of the tree.
   */
  public int getHeight() {
    return getLongestPathFromRootToAnyLeaf().size();
  }

  /**
   *
   * Get a list of all the paths (which is again a list of nodes along a path) from the root node to every leaf.
   *
   * @return List of paths.
   */
  public List<List<Node<T>>> getPathsFromRootToAnyLeaf() {
    List<List<Node<T>>> paths = new ArrayList<>();
    List<Node<T>> currentPath = new ArrayList<>();
    getPath(root, currentPath, paths);

    return paths;
  }

  private void getPath(Node<T> node, List<Node<T>> currentPath,
                       List<List<Node<T>>> paths) {
    if (currentPath == null)
      return;

    currentPath.add(node);

    if (node.getChildren().size() == 0) {
      // This is a leaf
      paths.add(clone(currentPath));
    }
    for (Node<T> child : node.getChildren())
      getPath(child, currentPath, paths);

    int index = currentPath.indexOf(node);
    for (int i = index; i < currentPath.size(); i++)
      currentPath.remove(index);
  }

  private List<Node<T>> clone(List<Node<T>> list) {
    final List<Node<T>> newList = new ArrayList<>();

    list.forEach(e -> newList.add(new Node<>(e)));

    return newList;
  }
}
