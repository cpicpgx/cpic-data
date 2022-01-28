package org.cpicpgx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileStoreClientTest {

  @Test
  void testEscapeUrl() {
    assertEquals("https://cpicpgx.org",
        FileStoreClient.escapeUrl("https://cpicpgx.org"));

    assertEquals("https://cpicpgx.org/test%20path.html",
        FileStoreClient.escapeUrl("https://cpicpgx.org/test path.html"));

    assertEquals(
        "https://files.cpicpgx.org/data/guideline/staging/1643371462/rosuvastatin%20recommendation.xlsx",
        FileStoreClient.escapeUrl("https://files.cpicpgx.org/data/guideline/staging/1643371462/rosuvastatin recommendation.xlsx"));
  }
}
