package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import se.sawano.java.text.AlphanumericComparator;

import java.util.Comparator;
import java.util.Locale;

public class ActivityScoreComparator implements Comparator<CharSequence> {
  private static final Comparator<CharSequence> sf_comparator = new ActivityScoreComparator();
  private static final Comparator<CharSequence> sf_anComparator = new AlphanumericComparator(Locale.ENGLISH).reversed();

  public static Comparator<CharSequence> getComparator() {
    return sf_comparator;
  }

  @Override
  public int compare(CharSequence o1, CharSequence o2) {
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

    return sf_anComparator.compare(o1, o2);
  }
}
