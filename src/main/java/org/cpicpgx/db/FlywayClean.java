package org.cpicpgx.db;

import org.flywaydb.core.Flyway;

/**
 * This class will clear the current Flyway database of all content
 */
public class FlywayClean {

  public static void main(String[] args) {
    Flyway flyway = Flyway.configure().dataSource(
        ConnectionFactory.getJdbcUrl(),
        ConnectionFactory.getUser(),
        ConnectionFactory.getPass()
    ).schemas(ConnectionFactory.getSchema()).defaultSchema(ConnectionFactory.getSchema()).load();
    flyway.clean();
  }
}
