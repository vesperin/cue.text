package com.vesperin.text.groups.wordset;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vesperin.text.Grouping;
import com.vesperin.text.Project;
import com.vesperin.text.Selection;
import com.vesperin.text.groups.Magnet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Huascar Sanchez
 */
public abstract class WordsetMagnet implements Magnet<Grouping.Groups, Project> {

  private static final int OVERLAP     = 3;
  private static final int MAX_OVERLAP = 30;
  private final int overlap;

  WordsetMagnet(){
    this(OVERLAP);
  }

  /**
   *
   * @param overlap overlapping factor (>= 3 and < 10).
   */
  private WordsetMagnet(int overlap){
    this.overlap = overlap;
  }

  @Override public Grouping.Groups apply(List<Project> projects) {
    final int threshold = Math.min(Math.max(Math.max(0, overlap), OVERLAP), MAX_OVERLAP);

    final Map<String, Project> map   = Maps.newHashMap();
    final Map<String, Set<String>> index = Maps.newHashMap();
    final Set<String> missed = Sets.newHashSet();

    projects.forEach(p -> map.put(p.name(), p));

    for(Project a : projects) {
      Project max = null;

      for(Project b: projects) {
        if(Objects.equals(a, b)) continue;

        if(Objects.isNull(max)){
          max = b;
        } else {

          if(Double.compare(score(a, b), score(a, max)) > 0 ){
            max = b;
          }
        }
      }

      if(Objects.isNull(max)) continue;

      populateIndex(threshold, index, missed, a, max);
    }

    if(!missed.isEmpty()){

      for(String each : missed){
        for(String key : index.keySet()){
          final Set<String> val = index.get(key);
          if(val.contains(each)) break;

          index.put(each, Sets.newHashSet(each));
        }
      }

    }

    final Set<Grouping.Group> groups = Sets.newHashSet();
    for(String key : index.keySet()){
      final Grouping.Group group = Grouping.newGroup();
      final Project head = map.get(key);
      final Set<Project> tail = index.get(key).stream()
        .map(map::get)
        .collect(Collectors.toSet());

      group.add(head);
      tail.forEach(group::add);

      groups.add(group);
    }


    return Grouping.Groups.of(groups.stream().collect(Collectors.toList()));
  }

  /**
   * Compares two projects and then produced a user-defined score.
   *
   * @param a first project
   * @param b second project
   * @return a user-defined score that compares these two projects.
   */
  protected abstract double score(Project a, Project b);


  private void populateIndex(int threshold, Map<String, Set<String>> index,
                             Set<String> missed, Project a, Project max) {

    final Set<Selection.Word> common = Sets.intersection(max.wordSet(), a.wordSet());

    if(common.size() > threshold){

      if(!index.containsKey(a.name())){

        if(index.containsKey(max.name())){
          final Set<String> other = index.get(max.name());
          if(!other.contains(a.name())) {
            index.get(max.name()).add(a.name());
          }
        } else {
          index.put(a.name(), Sets.newHashSet(max.name()));
        }
      } else {
        index.get(a.name()).add(max.name());
      }
    } else {
      if(!common.isEmpty()){
        index.put(a.name(), Sets.newHashSet(max.name()));
      } else {
        missed.add(a.name());
      }
    }
  }


  static double jaccard(Project a, Project b) {

    final Set<Selection.Word> intersect = Sets.intersection(a.wordSet(), b.wordSet());
    final Set<Selection.Word> union     = Sets.union(a.wordSet(), b.wordSet());

    final double topScore     = 1.0D * intersect.size();
    final double bottomScore  = 1.0D * union.size();

    return (topScore/bottomScore);

  }
}
