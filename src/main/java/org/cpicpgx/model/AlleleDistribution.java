package org.cpicpgx.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class AlleleDistribution {

  public static final String FREQ_FORMAT = "%.4f";
  private final String f_gene;
  private final String f_alleleName;
  private final Map<GnomadPopulation, BigDecimal> f_freqMap = new TreeMap<>();
  private final Map<GnomadPopulation, Integer> f_sizeMap = new TreeMap<>();

  public AlleleDistribution(String gene, String f_alleleName) {
    f_gene = gene;
    this.f_alleleName = f_alleleName;
  }

  public String toString() {
    return f_gene + " " + f_alleleName + " frequencies";
  }

  public BigDecimal getFreq(GnomadPopulation pop) {
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

  public BigDecimal getAvgFreqForGroup(String group) {
    List<GnomadPopulation> pops = f_freqMap.keySet().stream()
        .filter(p -> Objects.equals(p.getPgkbGroup(), group))
        .collect(Collectors.toList());

    if (pops.size() == 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal num = pops.stream()
        .map(p -> f_freqMap.get(p).multiply(new BigDecimal(f_sizeMap.get(p))))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    Integer den = pops.stream()
        .map(f_sizeMap::get)
        .reduce(0, Integer::sum);
    return num.divide(new BigDecimal(den), RoundingMode.HALF_EVEN);
  }

  public BigDecimal getMaxFreqForGroup(String group) {
    List<GnomadPopulation> pops = f_freqMap.keySet().stream()
        .filter(p -> Objects.equals(p.getPgkbGroup(), group))
        .collect(Collectors.toList());

    if (pops.size() == 0) {
      return BigDecimal.ZERO;
    }

    return pops.stream()
            .map(f_freqMap::get)
            .max(AlleleDistribution::compareBigDecimals)
            .orElse(BigDecimal.ZERO);
  }

  public BigDecimal getMinFreqForGroup(String group) {
    List<GnomadPopulation> pops = f_freqMap.keySet().stream()
            .filter(p -> Objects.equals(p.getPgkbGroup(), group))
            .collect(Collectors.toList());

    if (pops.size() == 0) {
      return BigDecimal.ZERO;
    }

    return pops.stream()
            .map(f_freqMap::get)
            .min(AlleleDistribution::compareBigDecimals)
            .orElse(BigDecimal.ZERO);
  }

  public void addSize(GnomadPopulation pop, Integer size) {
    f_sizeMap.put(pop, f_sizeMap.getOrDefault(pop, 0) + size);
  }

  public void set(GnomadPopulation pop, BigDecimal freq) {
    f_freqMap.put(pop, freq);
  }

  public String getAlleleName() {
    return f_alleleName;
  }


  private static int compareBigDecimals(BigDecimal o1, BigDecimal o2) {
    return o1.compareTo(o2);
  }
}
