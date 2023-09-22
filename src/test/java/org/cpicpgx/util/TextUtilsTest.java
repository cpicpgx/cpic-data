package org.cpicpgx.util;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class TextUtilsTest {

  @Test
  void testNormalize() {
    assertEquals("foo", TextUtils.normalize("foo"));
    assertEquals("\"wild-type\"", TextUtils.normalize("“wild-type”"));
    assertNull(TextUtils.normalize("     "));
    assertNull(TextUtils.normalize("  \t    \n  "));
    assertEquals("non-breaking space", TextUtils.normalize("\u00A0non-breaking\u00A0\u2007space\u00A0"));
  }

  private static final Supplier<Throwable> sf_fail = () -> new RuntimeException("Don't pass");

  @Test
  void testExtractPvid() throws Throwable {
    assertFalse(TextUtils.extractPvid("").isPresent());
    assertFalse(TextUtils.extractPvid(null).isPresent());
    assertFalse(TextUtils.extractPvid("       \n").isPresent());

    assertEquals("PV00001", TextUtils.extractPvid("PV00001").orElseThrow(sf_fail));
    assertEquals("PV00001", TextUtils.extractPvid("PV00001 foo").orElseThrow(sf_fail));
    assertEquals("PV00001", TextUtils.extractPvid("bar PV00001, foo").orElseThrow(sf_fail));
    assertFalse(TextUtils.extractPvid("foo").isPresent());
    assertFalse(TextUtils.extractPvid("PVOOOO1").isPresent());

    try {
      TextUtils.extractPvid("PV1 PV2");
      fail("Did not catch more than one PVID in string");
    } catch (RuntimeException ex) {
      // drop
    }
  }

  @Test
  void testNormalizeAlleleFunction() {
    assertEquals("", TextUtils.normalizeAlleleFunction(""));
    assertEquals("foo", TextUtils.normalizeAlleleFunction("foo"));
    assertEquals("foo function", TextUtils.normalizeAlleleFunction("foo function"));
    assertEquals("foo function", TextUtils.normalizeAlleleFunction("foo Function"));
  }

  @Test
  void testIsValidAlleleName() {
    // don't allow blank allele names
    assertFalse(TextUtils.isValidAlleleName(null));
    assertFalse(TextUtils.isValidAlleleName("   "));

    // don't allow slashes in allele names
    assertFalse(TextUtils.isValidAlleleName("foo/bar"));
    assertFalse(TextUtils.isValidAlleleName("/"));
    assertFalse(TextUtils.isValidAlleleName("/fooo"));
    assertFalse(TextUtils.isValidAlleleName("oooof/"));

    // disallow space-plus-space
    assertFalse(TextUtils.isValidAlleleName("Foo + Bar"));
    // all other uses of plus is ok
    assertTrue(TextUtils.isValidAlleleName("Foo+Bar"));
    assertTrue(TextUtils.isValidAlleleName("Foo+"));
    assertTrue(TextUtils.isValidAlleleName("+Bar"));
    assertTrue(TextUtils.isValidAlleleName("Foo +Bar"));

    // all other stuff should be fine
    assertTrue(TextUtils.isValidAlleleName("foo"));
    assertTrue(TextUtils.isValidAlleleName("*10"));
    assertTrue(TextUtils.isValidAlleleName("Foo-Bar"));
    assertTrue(TextUtils.isValidAlleleName("Foo…Bar"));
    assertTrue(TextUtils.isValidAlleleName("------"));
  }
}
