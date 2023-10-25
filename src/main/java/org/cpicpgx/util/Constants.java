package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public class Constants {
  public static final String INDETERMINATE = "Indeterminate";
  public static final Pattern INDETERMINATE_PATTERN = Pattern.compile("[Ii]ndeterminate");
  public static final String NA = "n/a";
  public static final Pattern NA_PATTERN = Pattern.compile("[Nn]/[Aa]");
  public static final String NO_RESULT = "No Result";
  public static final Pattern NO_RESULT_PATTERN = Pattern.compile("[Nn]o ([Rr]esult|[Tt]est)( on [Ff]ile)?");
  public static final String EXCEL_EXTENSION = ".xlsx";
  public static final String STRUCTURAL_VARIATION = "Structural Variation";
  public static final Pattern SINGLE_PATTERN = Pattern.compile("chr[MY]");

  /**
   * Detect if the text is "No Result" or some other capitalization variant
   * @param text text to test
   * @return true if text is "No Result" or close equivalent
   */
  public static boolean isNoResult(@Nullable String text) {
    return text != null && NO_RESULT_PATTERN.matcher(text).matches();
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
    return StringUtils.isBlank(value) || NA_PATTERN.matcher(StringUtils.stripToEmpty(value)).matches();
  }

  /**
   * Detect if a chromosome is single, and not double-stranded (M and Y)
   * @param chr chromosome name in form "chr##"
   * @return true if the chromosome is single stranded
   */
  public static boolean isSinglePloidy(String chr) {
    return StringUtils.isNotBlank(chr) && SINGLE_PATTERN.matcher(chr).matches();
  }
}
