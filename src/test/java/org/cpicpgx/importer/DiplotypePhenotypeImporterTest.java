package org.cpicpgx.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test methods used for diplotype-phenotype translation loading
 *
 * @author Ryan Whaley
 */
public class DiplotypePhenotypeImporterTest {
  
  @Test
  public void testFlipDip() {
    assertEquals("*2/*1", DiplotypePhenotypeImporter.flipDip("*1/*2"));
    assertEquals("*1/*1", DiplotypePhenotypeImporter.flipDip("*1/*1"));
  }
  
  @Test
  public void testIsHom() {
    assertTrue(DiplotypePhenotypeImporter.isHom("*1/*1"));
    assertFalse(DiplotypePhenotypeImporter.isHom("*1/*2"));
  }
}
