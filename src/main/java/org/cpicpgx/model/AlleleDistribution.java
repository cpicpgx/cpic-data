package org.cpicpgx.model;

import java.util.Map;
import java.util.TreeMap;

public class AlleleDistribution {

  public static final String FREQ_FORMAT = "%.4f";
  private final String f_gene;
  private final String f_alleleName;
  private final Map<GnomadPopulation, Double> f_freqMap = new TreeMap<>();
  private final Map<GnomadPopulation, Integer> f_sizeMap = new TreeMap<>();

  public AlleleDistribution(String gene, String f_alleleName) {
    f_gene = gene;
    this.f_alleleName = f_alleleName;
  }

  public String toString() {
    return f_gene + " " + f_alleleName + " frequencies";
  }

  public Double getFreq(GnomadPopulation pop) {
    return f_freqMap.get(pop);
  }

  public String getFreqAsString(GnomadPopulation pop) {
    if (f_freqMap.containsKey(pop) && f_freqMap.get(pop) != null) {
      return String.format(FREQ_FORMAT, f_freqMap.get(pop));
    } else {
      return "";
    }
  }

  public Integer getSize(GnomadPopulation pop) {
    return f_sizeMap.getOrDefault(pop, 0);
  }

  public void addSize(GnomadPopulation pop, Integer size) {
    f_sizeMap.put(pop, f_sizeMap.getOrDefault(pop, 0) + size);
  }

  public void set(GnomadPopulation pop, Double freq) {
    f_freqMap.put(pop, freq);
  }

  public String getAlleleName() {
    return f_alleleName;
  }
}
