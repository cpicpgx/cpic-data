package org.cpicpgx.stats.model;

/**
 * Types of statistics gathered by the system and will be written to the DB
 */
public enum StatisticType {
  COUNT_RELEASE("Count of releases", "GitHub releases"),
  COUNT_ALL_RELEASE("Count of releases and pre-releases", "GitHub releases"),
  COUNT_DB_DL("Count of database downloads", "GitHub releases"),
  SIZE_DB_TRANSFER("Download transfer (in bytes) of database files", "GitHub releases"),
  COUNT_FILES_PUBLISHED("Count of files published in last release", "GitHub releases"),

  SIZE_DB("Size of database (in bytes) on disk", "Database"),
  COUNT_DB_ROWS("Count of rows in the database", "Database"),
  COUNT_DB_TABLES("Count of tables in the database", "Database"),
  COUNT_GUIDELINES("Count of guidelines in the database", "Database"),
  COUNT_GUIDELINE_PUBLICATIONS("Count of the publications linked to guidelines", "Database"),
  COUNT_PAIRS("Count of current gene-drug pairs", "Database"),
  ;

  private final String f_description;
  private final String f_group;

  StatisticType(String description, String group) {
    f_description = description;
    f_group = group;
  }

  public String getDescription() {
    return f_description;
  }

  public String getGroup() {
    return f_group;
  }
}
