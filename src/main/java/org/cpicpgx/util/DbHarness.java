package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Helper class to interact with the database. Handles connection creation and closing any generated SQL
 * statements. Also include some caching for common queries like looking up drug IDs or guideline IDs.
 *
 * Extend this class in your own class and add your own write statements to it.
 */
public abstract class DbHarness implements AutoCloseable {
  private final List<AutoCloseable> closables = new ArrayList<>();
  private final Connection f_conn;
  private final FileType f_fileType;

  private final PreparedStatement drugLookup;
  private final Map<String, String> drugLookupCache = new HashMap<>();

  private final PreparedStatement geneLookup;
  private final Set<String> geneLookupCache = new HashSet<>();

  private final PreparedStatement guidelineLookup;
  private final Map<String, Integer> guidelineLookupCache = new HashMap<>();

  private final PreparedStatement insertChangeLog;

  public DbHarness(FileType type) throws SQLException {
    f_conn = ConnectionFactory.newConnection();
    closables.add(f_conn);
    f_fileType = type;

    //language=PostgreSQL
    drugLookup = prepare("select drugid from drug where lower(name)=?");
    //language=PostgreSQL
    geneLookup = prepare("select symbol from gene where symbol=?");
    //language=PostgreSQL
    guidelineLookup = prepare("select id from guideline where url=?");
    //language=PostgreSQL
    insertChangeLog = prepare("insert into change_log(entityId, note, type, date) values (?, ?, ?, ?)");
  }

  public PreparedStatement prepare(@Nonnull String sql) throws SQLException {
    PreparedStatement pstmt = f_conn.prepareStatement(sql);
    closables.add(pstmt);
    return pstmt;
  }

  public String lookupCachedDrug(String drugName) throws SQLException, NotFoundException {
    String normalizedName = StringUtils.lowerCase(StringUtils.stripToNull(drugName));
    if (normalizedName == null) return null;

    if (drugLookupCache.containsKey(normalizedName)) {
      return drugLookupCache.get(normalizedName);
    } else {
      drugLookup.setString(1, normalizedName);
      ResultSet rs = drugLookup.executeQuery();
      if (rs.next()) {
        String drugId = rs.getString(1);
        drugLookupCache.put(normalizedName, drugId);
        return drugId;
      } else {
        throw new NotFoundException("No drug found for " + drugName);
      }
    }
  }

  /**
   * Check whether a gene is already in the system, cache any found genes for faster lookup
   * @param geneSymbol the Gene to find as an HGNC symbol
   * @return true if this is a known gene, false if not found
   */
  public boolean lookupCachedGene(String geneSymbol) throws SQLException {
    if (geneSymbol == null) return false;
    if (geneLookupCache.contains(geneSymbol)) {
      return true;
    } else {
      geneLookup.setString(1, geneSymbol);
      ResultSet rs = geneLookup.executeQuery();
      if (rs.next()) {
        geneLookupCache.add(geneSymbol);
        return true;
      } else {
        return false;
      }
    }
  }

  public Integer lookupCachedGuideline(String url) throws SQLException {
    String normalizedUrl = StringUtils.stripToNull(url);
    if (normalizedUrl == null) return null;

    if (guidelineLookupCache.containsKey(normalizedUrl)) {
      return guidelineLookupCache.get(normalizedUrl);
    } else {
      guidelineLookup.setString(1, normalizedUrl);
      ResultSet rs = guidelineLookup.executeQuery();
      if (rs.next()) {
        int guidelineId = rs.getInt(1);
        guidelineLookupCache.put(normalizedUrl, guidelineId);
        return guidelineId;
      } else {
        throw new RuntimeException("No guideline found for " + url);
      }
    }
  }

  public void setNullableString(@Nonnull PreparedStatement stmt, int parameterIndex, @Nullable String value) throws SQLException {
    if (value == null) {
      stmt.setNull(parameterIndex, Types.VARCHAR);
    } else {
      if (value.equalsIgnoreCase(Constants.NA)) {
        stmt.setString(parameterIndex, Constants.NA);
      } else {
        stmt.setString(parameterIndex, value);
      }
    }
  }

  public void setNullableDate(@Nonnull PreparedStatement stmt, int parameterIndex, @Nullable Date value) throws SQLException {
    if (value == null) {
      stmt.setNull(parameterIndex, Types.DATE);
    } else {
      stmt.setDate(parameterIndex, new java.sql.Date(value.getTime()));
    }
  }

  public void setNullableInteger(@Nonnull PreparedStatement stmt, int parameterIndex, @Nullable Integer value) throws SQLException {
    if (value == null) {
      stmt.setNull(parameterIndex, Types.NUMERIC);
    } else {
      stmt.setInt(parameterIndex, value);
    }
  }

  public void setNullableArray(@Nonnull PreparedStatement stmt, int parameterIndex, @Nullable String[] values) throws SQLException {
    if (values == null || values.length == 0) {
      stmt.setNull(parameterIndex, Types.ARRAY);
    } else {
      stmt.setArray(parameterIndex, f_conn.createArrayOf("text", values));
    }
  }

  public void writeChangeLog(@Nullable String entityId, @Nonnull java.util.Date date, @Nonnull String note) throws SQLException {
    if (note.equalsIgnoreCase(AbstractWorkbook.LOG_FILE_CREATED)) return;

    this.insertChangeLog.clearParameters();
    if (StringUtils.isBlank(entityId)) {
      this.insertChangeLog.setNull(1, Types.VARCHAR);
    } else {
      this.insertChangeLog.setString(1, entityId);
    }
    this.insertChangeLog.setString(2, note);
    this.insertChangeLog.setString(3, f_fileType.name());
    this.insertChangeLog.setDate(4, new java.sql.Date(date.getTime()));
    this.insertChangeLog.executeUpdate();
  }

  public Array createArrayOf(String[] values) throws SQLException {
    return f_conn.createArrayOf("TEXT", values);
  }

  @Override
  public void close() {
    closables.forEach(c -> {
      try {
        c.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
