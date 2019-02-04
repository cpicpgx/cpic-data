package org.cpicpgx.util;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A genotype containing the phenotype description for 1 or more genes. Ensures only 1 phenotype is set per gene and 
 * will toString() in the same order no matter what order the genes are added in.
 *
 * @author Ryan Whaley
 */
public class Phenotype {
  
  private SortedMap<String,String> geneMap = new TreeMap<>();
  
  public Phenotype() {}

  /**
   * Add a gene phenotype to this Genotype
   * @param gene a gene symbol
   * @param phenotype a phenotype descriptor (e.g. "Extensive metabolizer")
   */
  public Phenotype with(String gene, String phenotype) {
    this.geneMap.put(gene, phenotype);
    return this;
  }

  /**
   * A JSON Array-like representation of this Genotype
   */
  public String toString() {
    return geneMap.keySet().stream()
        .map(k -> "\""+k+":"+geneMap.get(k)+"\"")
        .collect(Collectors.joining(", ", "[", "]"));
  }
}
