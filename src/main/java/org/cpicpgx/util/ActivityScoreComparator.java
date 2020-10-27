package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import se.sawano.java.text.AlphanumericComparator;

import java.util.Comparator;
import java.util.Locale;

/**
 * A comparator for activity score values. These values are strings but should contain only either the NA symbol or
 * decimal numerics with an optional prefix of a greater than symbol (≥).
 *
 * In general, values should be in descending numerical order with "greater than" values listed before their
 * non-prefixed equivalent values
 */
public class ActivityScoreComparator implements Comparator<String> {
  private static final Comparator<String> sf_comparator = new ActivityScoreComparator();
  private static final Comparator<CharSequence> sf_anComparator = new AlphanumericComparator(Locale.ENGLISH).reversed();
  private static final String gt = "≥";

  public static Comparator<String> getComparator() {
    return sf_comparator;
  }

  @Override
  public int compare(String o1, String o2) {
    if (StringUtils.isBlank(o1) && StringUtils.isBlank(o2)) {
      return 0;
    }
    if (StringUtils.isBlank(o1)) {
      return -1;
    }
    else if (StringUtils.isBlank(o2)) {
      return 1;
    }

    if (o1.equals(Constants.NA)) {
      return 1;
    }
    else if (o2.equals(Constants.NA)) {
      return -1;
    }

    // if both have a GT, compare as usual
    if (o1.startsWith(gt) && o2.startsWith(gt)) {
      return sf_anComparator.compare(o1, o2);
    }
    // if one value has a GT and the other doesn't
    else if (o1.startsWith(gt)) {
      String o1Stripped = o1.substring(1);
      // if they're the same number, then GT goes first
      if (o1Stripped.equals(o2)) {
        return -1;
      }
      // if they're not the same number, compare without the GT and follow normal rules
      else {
        return sf_anComparator.compare(o1Stripped, o2);
      }
    }
    else if (o2.startsWith(gt)) {
      String o2Stripped = o2.substring(1);
      if (o2Stripped.equals(o1)) {
        return 1;
      } else {
        return sf_anComparator.compare(o1, o2Stripped);
      }
    }
    // if neither has a GT
    return sf_anComparator.compare(o1, o2);
  }
}
