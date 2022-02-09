package org.cpicpgx.model;

import java.util.*;
import java.util.stream.Collectors;

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

  public Double getAvgFreqForGroup(String group) {
    List<GnomadPopulation> pops = f_freqMap.keySet().stream()
        .filter(p -> Objects.equals(p.getPgkbGroup(), group))
        .collect(Collectors.toList());

    if (pops.size() == 0) {
      return 0d;
    }

    double num = pops.stream()
        .map(p -> f_freqMap.get(p) * f_sizeMap.get(p))
        .reduce(0d, Double::sum);
    double den = pops.stream()
        .map(f_sizeMap::get)
        .reduce(0, Integer::sum);
    return num/den;
  }

  public Double getMaxFreqForGroup(String group) {
    List<GnomadPopulation> pops = f_freqMap.keySet().stream()
        .filter(p -> Objects.equals(p.getPgkbGroup(), group))
        .collect(Collectors.toList());

    if (pops.size() == 0) {
      return 0d;
    }

    return pops.stream()
        .map(f_freqMap::get)
        .max(Double::compare).orElse(0d);
  }

  public Double getMinFreqForGroup(String group) {
    List<GnomadPopulation> pops = f_freqMap.keySet().stream()
        .filter(p -> Objects.equals(p.getPgkbGroup(), group))
        .collect(Collectors.toList());

    if (pops.size() == 0) {
      return 0d;
    }

    return pops.stream()
        .map(f_freqMap::get)
        .min(Double::compare).orElse(0d);
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
