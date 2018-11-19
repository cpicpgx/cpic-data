package org.cpicpgx.db;

import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Helper methods for doing lookups from the Database
 *
 * @author Ryan Whaley
 */
public class DbLookup {

  /**
   * Look up a drug ID from a drug name
   * @param conn an open database connection
   * @param name a drug name, case insensitive
   * @return an Optional String of the drug ID
   * @throws SQLException can occur from bad query syntax
   */
  public static Optional<String> getDrugByName(Connection conn, String name) throws SQLException {
    if (StringUtils.isBlank(name)) {
      return Optional.empty();
    }
    
    try (PreparedStatement lookup = conn.prepareStatement(
        "select drugid from drug where name=?",
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY)) {
      lookup.setString(1, name);
      try (ResultSet rs = lookup.executeQuery()) {
        if (rs.first()) {
          return Optional.of(rs.getString(1));
        } else {
          return Optional.empty();
        }
      }
    }
  }
  
  
  
}
