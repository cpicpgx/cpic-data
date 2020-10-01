package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class Constants {
  public static final String INDETERMINATE = "Indeterminate";
  public static final Pattern INDETERMINATE_PATTERN = Pattern.compile("[Ii]determinate]");
  public static final String NA = "n/a";
  public static final String NO_RESULT = "No Result";
  public static final Pattern NO_RESULT_PATTERN = Pattern.compile("[Nn]o ([Rr]esult|[Tt]est)( on [Ff]ile)?");
  public static final String EXCEL_EXTENSION = ".xlsx";

  /**
   * Detect if the text is "No Result" or some other capitalization variant
   * @param text text to test
   * @return true if text is "No Result" or close equivalent
   */
  public static boolean isNoResult(String text) {
    return NO_RESULT_PATTERN.matcher(text).matches();
  }

  /**
   * Detect fi the text is "Indeterminate"
   * @param text text to test
   * @return true if this text is equivalent to "Indeterminate"
   */
  public static boolean isIndeterminate(String text) {
    return INDETERMINATE_PATTERN.matcher(StringUtils.stripToEmpty(text)).matches();
  }

  /**
   * Detect an unspecified value
   * @param value the text to test
   * @return true if the text is either blank or "n/a"
   */
  public static boolean isUnspecified(String value) {
    return StringUtils.isBlank(value) || value.equals(NA);
  }
}
