package org.cpicpgx.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ryan Whaley
 */
public class AlleleDefinitionImporterTest {
  
  @Test
  public void testTrim() {
    assertEquals(AlleleDefinitionImporter.trim(" CYP2C19"), "CYP2C19");
  }
}
