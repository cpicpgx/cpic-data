package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A class for storing Publication information in the database 
 *
 * @author Ryan Whaley
 */
public class PublicationCatalog {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private Connection conn;
  private PreparedStatement lookupStmt;
  private PreparedStatement insertStmt;
  private Set<String> checkedPmids = new HashSet<>();

  PublicationCatalog(Connection conn) throws SQLException {
    this.conn = conn;
    lookupStmt = conn.prepareStatement("select id from publication p where p.pmid=?",
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    insertStmt = conn.prepareStatement("insert into publication(pmid, year, authors) values (?, ?, ?)");
  }

  /**
   * This will check the database to see if we have data on the given PMID. If not, it will add the given data. If the 
   * information already exists or if the given PMID is not properly formatted it will do nothing.
   * @param pmid a PubMed identifier
   * @param year the 4 digit year as a string
   * @param author the name of the first author
   * @throws SQLException can occur from a bad insert into the publications table
   */
  public void add(String pmid, String year, String author) throws SQLException {
    if (StringUtils.isBlank(pmid)) return;
    if (!Pattern.compile("\\d+").matcher(pmid).matches()) {
      sf_logger.warn("Bad PMID value {}", pmid);
      return;
    }
    
    int pubYear;
    try {
      pubYear = Integer.parseInt(year);
    } catch (NumberFormatException ex) {
      throw new RuntimeException(String.format("%s has an non-integer year associated with it [%s]", pmid, year), ex);
    }
    
    if (checkedPmids.contains(pmid)) return;
    
    lookupStmt.setString(1, pmid);
    try (ResultSet rs = lookupStmt.executeQuery()) {
      if (rs.first()) {
        checkedPmids.add(pmid);
      } else {
        insertStmt.setString(1, pmid);
        insertStmt.setInt(2, pubYear);
        insertStmt.setArray(3, this.conn.createArrayOf("text", new String[]{author}));
        insertStmt.executeUpdate();
        checkedPmids.add(pmid);
      }
    }
  }
}
