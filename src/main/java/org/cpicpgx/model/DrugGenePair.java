package org.cpicpgx.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;


/**
 * Represents a Drug-Gene pair. There can be more than 1 or more genes. The pair can also, optionally, have no
 * recommendation.
 */
public class DrugGenePair {

  private final String f_drugName;
  private String drugId;
  private final Set<String> f_genes;
  private final boolean f_hasRecommendation;

  public DrugGenePair(String drugName, String[] genes, boolean hasRecommendation) {
    f_drugName = drugName;
    f_genes = new TreeSet<>();
    Stream.of(genes)
        .filter(StringUtils::isNotBlank)
        .forEach(f_genes::add);
    f_hasRecommendation = hasRecommendation;
  }

  public DrugGenePair(String pairText, boolean hasRecommendation) {
    String[] pairTokens = pairText.split("\\|");
    if (pairTokens.length < 2) {
      throw new RuntimeException("Need at least one drug and one gene");
    }

    f_drugName = StringUtils.trim(pairTokens[0]);

    f_genes = new TreeSet<>();
    String[] geneTokens = Arrays.copyOfRange(pairTokens, 1, pairTokens.length);
    Stream.of(geneTokens)
        .map(StringUtils::trimToNull)
        .filter(StringUtils::isNotBlank)
        .forEach(f_genes::add);

    f_hasRecommendation = hasRecommendation;
  }

  public String getDrugName() {
    return f_drugName;
  }

  public String getDrugId() {
    return drugId;
  }

  public void setDrugId(String drugId) {
    this.drugId = drugId;
  }

  public Set<String> getGenes() {
    return f_genes;
  }

  public boolean hasRecommendation() {
    return f_hasRecommendation;
  }
}
