package org.cpicpgx.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FunctionReferenceImporterTest {

  @Test
  public void testParseAlleleDefinitionName() {
    assertNull(FunctionReferenceImporter.parseAlleleDefinitionName(""));
    assertNull(FunctionReferenceImporter.parseAlleleDefinitionName("    "));

    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2"));
    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2xN"));
    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2x3"));
    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2≥2"));
    assertEquals("c.61G>T", FunctionReferenceImporter.parseAlleleDefinitionName("c.61G>T"));
    assertEquals("c.61G>T", FunctionReferenceImporter.parseAlleleDefinitionName("c.61G>Tx2"));
    assertEquals("c.61G>T", FunctionReferenceImporter.parseAlleleDefinitionName("c.61G>T   x2"));
    assertEquals("H1", FunctionReferenceImporter.parseAlleleDefinitionName("H1"));
  }
}
