package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A class for storing Publication information in the database 
 *
 * @author Ryan Whaley
 */
@SuppressWarnings("SpellCheckingInspection")
public class PublicationCatalog {
  private Connection conn;
  private PreparedStatement pmidLookupStmt;
  private PreparedStatement pmcidLookupStmt;
  private PreparedStatement urlLookupStmt;
  private PreparedStatement doiLookupStmt;
  private PreparedStatement pmidInsertStmt;
  private PreparedStatement pmcidInsertStmt;
  private PreparedStatement doiInsertStmt;
  private PreparedStatement urlInsertStmt;
  private Map<String, Integer> checkedIds = new HashMap<>();

  PublicationCatalog(Connection conn) throws SQLException {
    this.conn = conn;
    pmidLookupStmt = conn.prepareStatement("select id from publication p where p.pmid=?",
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    pmcidLookupStmt = conn.prepareStatement("select id from publication p where p.pmcid=?",
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    urlLookupStmt = conn.prepareStatement("select id from publication p where p.url=?",
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    doiLookupStmt = conn.prepareStatement("select id from publication p where p.doi=?",
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY);
    pmidInsertStmt = conn.prepareStatement("insert into publication(pmid, year, authors) values (?, ?, ?) returning id");
    pmcidInsertStmt = conn.prepareStatement("insert into publication(pmcid, year, authors) values (?, ?, ?) returning id");
    doiInsertStmt = conn.prepareStatement("insert into publication(doi, year, authors) values (?, ?, ?) returning id");
    urlInsertStmt = conn.prepareStatement("insert into publication(url, year, authors) values (?, ?, ?) returning id");
  }

  /**
   * This will check the database to see if we have data on the given PMID. If not, it will add the given data. If the 
   * information already exists or if the given PMID is not properly formatted it will do nothing.
   * @param externalId an identifier for the publication (PMID, PMCID, URL, or DOI)
   * @param year the 4 digit year as a string
   * @param author the name of the first author
   * @throws SQLException can occur from a bad insert into the publications table
   */
  Integer lookupId(String externalId, String year, String author) throws SQLException {
    if (StringUtils.isBlank(externalId)) return null;
    
    Integer pubYear = null;
    if (StringUtils.isNotBlank(year)) {
      try {
        pubYear = Integer.parseInt(year);
      } catch (NumberFormatException ex) {
        throw new RuntimeException(String.format("%s has an non-integer year associated with it [%s]", externalId, year), ex);
      }
    }
    
    Integer pubId = checkedIds.get(externalId);

    if (pubId == null) {
      PreparedStatement lookupStmt;
      PreparedStatement insertStmt;
      if (externalId.matches("^PMC\\d+$")) {
        lookupStmt = pmcidLookupStmt;
        insertStmt = pmcidInsertStmt;
      } else if (externalId.matches("^doi:.+")) {
        lookupStmt = doiLookupStmt;
        insertStmt = doiInsertStmt;
      } else if (externalId.matches("^https?:.+")) {
        lookupStmt = urlLookupStmt;
        insertStmt = urlInsertStmt;
      } else {
        lookupStmt = pmidLookupStmt;
        insertStmt = pmidInsertStmt;
      }

      Optional<Integer> optionalId = lookup(lookupStmt, externalId);
      if (optionalId.isPresent()) {
        pubId = optionalId.get();
        checkedIds.put(externalId, pubId);
      }

      if (!checkedIds.containsKey(externalId)) {
        pubId = add(insertStmt, externalId, pubYear, new String[]{author});
        checkedIds.put(externalId, pubId);
      }
    }
    
    return pubId;
  }
  
  private Optional<Integer> lookup(PreparedStatement lookupStmt, String externalId) throws SQLException {
    lookupStmt.clearParameters();
    lookupStmt.setString(1, externalId);
    try (ResultSet rs = lookupStmt.executeQuery()) {
      if (rs.first()) {
        return Optional.of(rs.getInt(1));
      } else {
        return Optional.empty();
      }
    }
  }
  
  private Integer add(PreparedStatement insertStmt, String externalId, Integer year, String[] authors) throws SQLException {
    insertStmt.setString(1, externalId);
    if (year != null) {
      insertStmt.setInt(2, year);
    } else {
      insertStmt.setNull(2, Types.INTEGER);
    }
    insertStmt.setArray(3, this.conn.createArrayOf("text", authors));
    
    try (ResultSet rs = insertStmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        throw new RuntimeException("Could not add new publication");
      }
    }
  }
}
