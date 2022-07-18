package org.cpicpgx.stats.model;

/**
 * Types of statistics gathered by the system and will be written to the DB
 */
public enum StatisticType {
  COUNT_RELEASE("Count of releases"),
  COUNT_ALL_RELEASE("Count of releases and pre-releases"),
  COUNT_DB_DL("Count of database downloads"),
  SIZE_DB_TRANSFER("Download transfer (in bytes) of database files"),
  SIZE_DB("Size of database (in bytes) on disk"),
  COUNT_DB_ROWS("Count of rows in the database"),
  COUNT_DB_TABLES("Count of tables in the database"),
  COUNT_FILES_PUBLISHED("Count of files published in last release"),
  COUNT_GUIDELINES("Count of guidelines in the database"),
  COUNT_GUIDELINE_PUBLICATIONS("Count of the publications linked to guidelines"),
  COUNT_PAIRS("Count of current gene-drug pairs"),
  ;

  private final String f_description;

  StatisticType(String description) {
    f_description = description;
  }

  public String getDescription() {
    return f_description;
  }
}
