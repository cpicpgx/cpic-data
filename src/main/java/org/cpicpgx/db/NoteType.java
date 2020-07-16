package org.cpicpgx.db;

/**
 * The different types of notes that are possible in the <code>gene_note</code> table.
 *
 * @author Ryan Whaley
 */
public enum NoteType {
  
  ALLELE_DEFINITION,
  ALLELE_FREQUENCY,
  CDS,
  FUNCTION_REFERENCE,
  RECOMMENDATIONS,
  TEST_ALERT
}
