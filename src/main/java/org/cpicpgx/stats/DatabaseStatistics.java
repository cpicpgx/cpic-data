package org.cpicpgx.stats;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.stats.model.StatisticType;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to gather statistics from the PostgreSQL database
 */
public class DatabaseStatistics implements AbstractStatistics {
  private static long queryCount(Connection conn, @Language("PostgreSQL") String sql) throws SQLException {
    long count;
    try (ResultSet rs = conn.prepareStatement(sql).executeQuery()) {
      if (rs.next()) {
        count = rs.getLong(1);
      } else {
        count = 0;
      }
    }
    return count;
  }

  @Override
  public Map<StatisticType, Long> gather() {
    Map<StatisticType, Long> stats = new HashMap<>();

    try (Connection conn = ConnectionFactory.newConnection()) {
      stats.put(
          StatisticType.COUNT_DB_TABLES,
          queryCount(conn, "select count(*) from pg_tables where schemaname='cpic'")
      );
      stats.put(
          StatisticType.COUNT_DB_ROWS,
          queryCount(conn, "select sum(reltuples) " +
          "from pg_class c join pg_namespace pn on c.relnamespace = pn.oid " +
          "where c.relkind='r' and nspname in ('cpic') and reltuples >= 0")
      );
      stats.put(
          StatisticType.SIZE_DB,
          queryCount(conn, "select pg_database_size('cpic')")
      );

      stats.put(
          StatisticType.COUNT_FILES_PUBLISHED,
          queryCount(conn, "select count(*) from file_artifact")
      );

      stats.put(
          StatisticType.COUNT_GUIDELINES,
          queryCount(conn, "select count(*) from guideline")
      );

      stats.put(
          StatisticType.COUNT_GUIDELINE_PUBLICATIONS,
          queryCount(conn, "select count(distinct p.id) from guideline g join publication p on g.id = p.guidelineid")
      );

      stats.put(
          StatisticType.COUNT_PAIRS,
          queryCount(conn, "select count(*) from pair where removed is false")
      );
    } catch (SQLException ex) {
      throw new RuntimeException("Error querying DB", ex);
    }

    return stats;
  }
}
