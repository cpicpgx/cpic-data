package org.cpicpgx.importer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ryan Whaley
 */
public class AlleleDefinitionImporterTest {
  
  @Test
  public void testTrim() {
    assertEquals(AlleleDefinitionImporter.trim(" CYP2C19"), "CYP2C19");
  }
}
