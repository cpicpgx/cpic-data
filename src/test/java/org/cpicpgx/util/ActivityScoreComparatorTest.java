package org.cpicpgx.util;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import se.sawano.java.text.AlphanumericComparator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActivityScoreComparatorTest {

  private static final String gtSix = "≥6";
  private static final String gtOne = "≥1";
  private static final String three = "3";
  private static final String onePointFive = "1.5";
  private static final String six = "6";
  private static final String gtFive = "≥5";

  @Test
  void test() {
    assertTrue(ActivityScoreComparator.getComparator().compare(gtSix, three) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(gtSix, six) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(six, three) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(three, onePointFive) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(three, gtOne) < 0);
    assertTrue(ActivityScoreComparator.getComparator().compare(three, Constants.NA) < 0);

    Set<String> example = new TreeSet<>(ActivityScoreComparator.getComparator());
    example.add(three);
    example.add(Constants.NA);
    example.add(gtSix);

    Iterator<String> it = example.iterator();
    assertEquals(gtSix, it.next());
    assertEquals(three, it.next());
    assertEquals(Constants.NA, it.next());

    List<String> exampleList = Lists.newArrayList(six, gtSix, gtFive);
    exampleList.sort(ActivityScoreComparator.getComparator());
    assertEquals(gtSix, exampleList.get(0));
    assertEquals(six, exampleList.get(1));
    assertEquals(gtFive, exampleList.get(2));
  }
}
