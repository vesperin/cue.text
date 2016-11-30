package com.vesperin.text;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vesperin.text.Selection.Word;
import com.vesperin.text.utils.Node;
import com.vesperin.text.utils.Tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public class Partitions {
  private Partitions(){}

  /**
   * Generates a cluster tree from a list of projects.
   *
   * @param projectList the list of projects.
   * @param <T> the type of elements in a project
   * @return a new cluster tree
   */
  public static <T> Tree<Partition<T>> buildClusterTree(List<Project<T>> projectList){
    final Tree<Partition<T>>        clusterTree = new Tree<>();
    final Deque<Project<T>>         projects    = buildProjectQueue(projectList);
    final List<Node<Partition<T>>>  visited     = Lists.newArrayList();

    while(!projects.isEmpty()){

      final Project<T> current = projects.remove();
      if(visited.isEmpty()){

        final Partition<T> root = Partition.newPartition(current.name());
        final Set<Project<T>> projectSet = projectList.stream().collect(Collectors.toSet());
        root.addAll(projectSet);
        root.copiesAll(current.wordSet());

        final Node<Partition<T>> rootNode = Node.newNode(root);

        clusterTree.setRoot(rootNode);
        visited.add(rootNode);

      } else {
        final List<Node<Partition<T>>>  compare = Lists.newArrayList(visited);

        for (Node<Partition<T>> each : compare){

          final Partition<T>  data      = each.getData();
          final Set<Word>     sharedSet = Sets.intersection(current.wordSet(), data.wordSet());

          if(!sharedSet.isEmpty()){ // connect projects via intersection

            final Partition<T> target = Partition.newPartition("(" + data.label() + " ∩ " + current.name() + ")");
            // copies the shared words to the new partition
            target.copiesAll(sharedSet);

            final Set<Project<T>> newPros = Sets.newHashSet();
            newPros.addAll(data.projectSet());
            newPros.add(current);

            target.addAllIff(newPros);

            final Node<Partition<T>> targetNode = Node.newNode(target);
            each.addChild(targetNode);

            visited.add(targetNode);

          }
        }

      }
    }


    return clusterTree;
  }

  private static <T> Deque<Project<T>> buildProjectQueue(List<Project<T>> projectList){
    final Deque<Project<T>>   projects    = new ArrayDeque<>();

    // Initialize root of cluster tree (union of all words in these projects)
    final StringBuilder         name      = new StringBuilder(projectList.size() * 1000);
    final Iterator<Project<T>>  iterator  = projectList.iterator();

    Set<Word>    master  = Sets.newHashSet();

    while(iterator.hasNext()){
      final Project<T> each = iterator.next();

      name.append(each.name());
      if(iterator.hasNext()){
        name.append(" ∪ ");
      }

      master = Sets.union(master, each.wordSet());

    }

    final Project<T> first = Project.createProject("(" + name.toString() + ")", master);
    projects.add(first);


    // Initialize projects
    projectList.forEach(projects::add);

    return projects;
  }


}
