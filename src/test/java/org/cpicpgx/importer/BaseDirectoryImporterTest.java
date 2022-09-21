package org.cpicpgx.importer;

import org.cpicpgx.util.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BaseDirectoryImporterTest {

  @Test
  void testNormalizeScore() {
    assertEquals("1.0",      BaseDirectoryImporter.normalizeScore("1"));
    assertEquals("2.0",      BaseDirectoryImporter.normalizeScore("2.0"));
    assertEquals("≥3.0",     BaseDirectoryImporter.normalizeScore("≥3"));
    assertEquals("≥4.0",     BaseDirectoryImporter.normalizeScore("≥4.0"));
    assertEquals(Constants.NA,        BaseDirectoryImporter.normalizeScore("n/a"));
    assertEquals(Constants.NA,        BaseDirectoryImporter.normalizeScore("N/A"));
    assertEquals(Constants.NO_RESULT, BaseDirectoryImporter.normalizeScore("no result on file"));
    assertEquals(Constants.NO_RESULT, BaseDirectoryImporter.normalizeScore("no test on file"));
    assertEquals(Constants.NO_RESULT, BaseDirectoryImporter.normalizeScore("No result"));

    try {
      BaseDirectoryImporter.normalizeScore("foo bar");
      fail("should have failed on bad score format");
    } catch (Exception e) {
      // ignore
    }
  }
}
