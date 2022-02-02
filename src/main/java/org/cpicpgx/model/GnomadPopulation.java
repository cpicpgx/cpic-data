package org.cpicpgx.model;

public enum GnomadPopulation {
  ALL("All populations"),
  AFR("African/African American"),
  AMI("Amish"),
  AMR ("Latino"),
  ASJ("Ashkenazi Jewish"),
  EAS("East Asian"),
  FIN("Finnish"),
  NFE("Non-Finnish European"),
  SAS("South Asian"),
  OTH("Other");

  private final String f_name;

  GnomadPopulation(String name) {
    f_name = name;
  }

  public String getName() {
    return f_name;
  }

  public boolean isNotSummary() {
    return this != ALL;
  }
}
