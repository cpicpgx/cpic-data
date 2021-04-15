package org.cpicpgx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TextUtilsTest {

  @Test
  void testNormalize() {
    assertEquals("foo", TextUtils.normalize("foo"));
    assertEquals("\"wild-type\"", TextUtils.normalize("“wild-type”"));
    assertNull(TextUtils.normalize("     "));
    assertNull(TextUtils.normalize("  \t    \n  "));
    assertEquals("non-breaking space", TextUtils.normalize("\u00A0non-breaking\u00A0\u2007space\u00A0"));
  }
}
