package org.cpicpgx.importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class AlleleDefinitionImporterTest {
  @Test
  void testCheckPosition() {
    AlleleDefinitionImporter imp = new AlleleDefinitionImporter();
    imp.checkPosition("g.1111C>T");

    try {
      imp.checkPosition("g.2222G>W");
      fail("Should throw an exception since there's a wobble code");
    } catch (RuntimeException ex) {
      // swallow the exception since we expect it
    }
  }
}
