package org.cpicpgx.importer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
