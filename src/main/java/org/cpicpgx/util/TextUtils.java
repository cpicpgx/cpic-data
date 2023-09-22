package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {
  private static final Pattern sf_badFunction = Pattern.compile(" Function$");
  private static final Pattern sf_badAlleleNamePattern = Pattern.compile("( \\+ |/)");

  /**
   * Normalize problem characters in Strings that text content
   * @param text nullable text to alter
   * @return normalized text, nullable
   */
  public static String normalize(String text) {
    if (text != null) {
      text = text.replaceAll("[\u2018\u2019\u201B]", "'"); // Office-style slanted single quotes
      text = text.replaceAll("[\u201C\u201D]", "\"");      // Office-style slanted quotes
      text = text.replaceAll("\\h+"," ");                  // non-breaking spaces
      text = StringUtils.stripToNull(text);                                  // trim whitespace and default to null
    }
    return text;
  }

  public static boolean isValidAlleleName(String text) {
    return StringUtils.isNotBlank(text) && !sf_badAlleleNamePattern.matcher(text).find();
  }

  /**
   * Normalize function text coming in from user-generated text. This will standardize the case of the word "function".
   * @param text a function description
   * @return a normalized version of the function description
   */
  public static String normalizeAlleleFunction(String text) {
    if (StringUtils.isBlank(text)) {
      return text;
    }
    Matcher m = sf_badFunction.matcher(text);
    if (m.find()) {
      return m.replaceAll(" function");
    }
    else {
      return text;
    }
  }

  private static final Pattern PVID_PATTERN = Pattern.compile("PV\\d+");

  /**
   * Find a single PharmVar ID (PVID) in the given text.
   * @param text nullable text to look for PVID in
   * @return an Optional containing the PVID if found
   * @throws RuntimeException when more than one PharmVar ID is in the text
   */
  public static Optional<String> extractPvid(String text) {
    if (StringUtils.isBlank(text)) {
      return Optional.empty();
    }

    Matcher m = PVID_PATTERN.matcher(text);
    if (m.find()) {
      String pvid = m.group(0);
      if (m.find()) {
        throw new RuntimeException("More than one PVID for [" + text + "]");
      }
      return Optional.of(pvid);
    }
    return Optional.empty();
  }
}
