package org.cpicpgx.db;

/**
 * This enum contains the different values for indicating how to lookup information for specific diplotypes of a gene.
 *
 * This applies to things like recommendations and CDS language.
 *
 * {@link LookupMethod#ACTIVITY_SCORE} Use the activity scores assigned to the diplotype when looking up information
 * {@link LookupMethod#FUNCTION} Use the functions assigned to the diplotype when looking up information
 */
public enum LookupMethod {
  ACTIVITY_SCORE,
  FUNCTION
}
