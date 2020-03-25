package org.cpicpgx.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * This class is a factory for creating new {@link Connection} objects for the Postgres database.
 *
 * @author Ryan Whaley
 */
public class ConnectionFactory {
  private static final String sf_dbUrl = "jdbc:postgresql://%s/cpic";
  private static ResourceBundle resources = ResourceBundle.getBundle("cpicData");
  private static String sf_host;
  private static String sf_user;
  private static String sf_pass;
  static {
    sf_host = resources.getString("db.host");
    sf_user = resources.getString("db.user");
    sf_pass = resources.getString("db.pass");
  }

  /**
   * Makes a new {@link Connection}. All connection information comes from the <code>cpicData.properties</code> file.
   * @return a new, opened JDBC database {@link Connection}
   * @throws SQLException can occur if there is a problem connecting to the database
   */
  public static Connection newConnection() throws SQLException {
    return DriverManager.getConnection(String.format(makeJdbcUrl(), sf_host), sf_user, sf_pass);
  }
  
  static String makeJdbcUrl() {
    return String.format(sf_dbUrl, sf_host);
  }

  static String getJdbcAddress() {
    return String.format("jdbc:postgresql://%s/cpic", sf_host);
  }

  static String getUser() {
    return sf_user;
  }

  static String getPass() {
    return sf_pass;
  }
}
