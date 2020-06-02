package org.cpicpgx.db;

import com.google.common.base.MoreObjects;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class is a factory for creating new {@link Connection} objects for the Postgres database.
 *
 * @author Ryan Whaley
 */
public class ConnectionFactory {
  private static final String sf_dbUrl = "jdbc:postgresql://%s/%s";
  private static final String sf_host = MoreObjects.firstNonNull(System.getenv("CPIC_HOST"), "localhost");
  private static final String sf_user = MoreObjects.firstNonNull(System.getenv("CPIC_USER"), "cpic");
  private static final String sf_pass = MoreObjects.firstNonNull(System.getenv("CPIC_PASS"), "");
  private static final String sf_db = MoreObjects.firstNonNull(System.getenv("CPIC_DB"), "cpic");

  /**
   * Makes a new {@link Connection}. All connection information defaults to local DB but also can override with environment variables.
   * @return a new, opened JDBC database {@link Connection}
   * @throws SQLException can occur if there is a problem connecting to the database
   */
  public static Connection newConnection() throws SQLException {
    return DriverManager.getConnection(getJdbcUrl(), sf_user, sf_pass);
  }
  
  static String getJdbcUrl() {
    return String.format(sf_dbUrl, sf_host, sf_db);
  }

  static String getUser() {
    return sf_user;
  }

  static String getPass() {
    return sf_pass;
  }
}
