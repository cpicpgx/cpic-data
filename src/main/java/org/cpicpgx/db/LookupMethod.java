package org.cpicpgx.db;

/**
 * This enum contains the different values for indicating how to lookup information for specific diplotypes of a gene.
 *
 * This applies to things like recommendations and CDS language.
 *
 * {@link LookupMethod#ACTIVITY_SCORE} Use the activity scores assigned to the diplotype when looking up information
 * {@link LookupMethod#PHENOTYPE} Use the phenotype of the diplotype when looking up information
 * {@link LookupMethod#ALLELE_STATUS} Use the status of whether an allele is present or not to look up information
 */
public enum LookupMethod {
  ACTIVITY_SCORE,
  PHENOTYPE,
  ALLELE_STATUS
}
