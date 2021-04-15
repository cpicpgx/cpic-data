package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;

public class TextUtils {

  /**
   * Normalize problem characters in Strings that text content
   * @param text nullable text to alter
   * @return normalized text, nullable
   */
  public static String normalize(String text) {
    if (text != null) {
      text = text.replaceAll("[“”]", "\"");            // Office-style slanted quotes
      text = text.replaceAll("\\h+"," "); // non-breaking spaces
      text = StringUtils.stripToNull(text);         // trim whitespace and default to null
    }
    return text;
  }
}
