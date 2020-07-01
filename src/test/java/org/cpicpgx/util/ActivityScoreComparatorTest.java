package org.cpicpgx.util;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActivityScoreComparatorTest {

  private static final String t1 = "≥6";
  private static final String t4 = "≥1";
  private static final String t2 = "3";
  private static final String t3 = "1.5";

  @Test
  void test() {
    assertTrue(ActivityScoreComparator.getComparator().compare(t1, t2) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(t2, t3) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(t4, t2) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(t2, Constants.NA) < 0);

    Set<String> example = new TreeSet<>(ActivityScoreComparator.getComparator());
    example.add(t2);
    example.add(Constants.NA);
    example.add(t1);

    Iterator<String> it = example.iterator();
    assertEquals(t1, it.next());
    assertEquals(t2, it.next());
    assertEquals(Constants.NA, it.next());
  }
}
