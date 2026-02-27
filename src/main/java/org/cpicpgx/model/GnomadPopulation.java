package org.cpicpgx.model;

import java.util.*;
import java.util.stream.Collectors;

public enum GnomadPopulation {
  ALL("All populations", null),
  AFR("African/African American", "Sub-Saharan African"),
  AMI("Amish", null),
  AMR ("Latino", "Latino"),
  ASJ("Ashkenazi Jewish", "Near Eastern"),
  EAS("East Asian", "East Asian"),
  FIN("Finnish", "European"),
  MID("Middle Eastern", "Near Eastern"),
  NFE("Non-Finnish European", "European"),
  SAS("South Asian", "Central/South Asian"),
  OTH("Other", null);

  public static final String GNOMAD_VERSION = "GnomAD v2.1.1";
  private final String f_name;
  private final String f_cpgxGroup;

  GnomadPopulation(String name, String cpgxGroup) {
    f_name = name;
    f_cpgxGroup = cpgxGroup;
  }

  /**
   * Gets the spelled-out population name, not the 3-letter code. For the code, use the {@link GnomadPopulation#name()}
   * method.
   * @return a String name
   */
  public String getName() {
    return f_name;
  }

  /**
   * Gets the gnomad version number and uses the 3-letter code for the population
   * @return a String name
   */
  public String getVersionedName() {
    return String.format("%s population %s", GNOMAD_VERSION, this.name());
  }

  public boolean isNotSummary() {
    return this != ALL;
  }

  public String getCpgxGroup() {
    return f_cpgxGroup;
  }

  public static List<String> getCpgxGroups() {
    return Arrays.stream(GnomadPopulation.values())
        .map(GnomadPopulation::getCpgxGroup)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  public static List<GnomadPopulation> getGnomadsForCpgx(String cpgxGroup) {
    return Arrays.stream(GnomadPopulation.values())
        .filter(p -> Objects.equals(p.getCpgxGroup(), cpgxGroup))
        .collect(Collectors.toList());
  }
}
