package org.cpicpgx.db;

import org.flywaydb.core.Flyway;

/**
 * This class will clear the current Flyway database of all content
 */
public class FlywayClean {

  public static void main(String[] args) {
    Flyway flyway = Flyway.configure().dataSource(
        ConnectionFactory.getJdbcAddress(),
        ConnectionFactory.getUser(),
        ConnectionFactory.getPass()
    ).load();
    flyway.clean();
  }
}
