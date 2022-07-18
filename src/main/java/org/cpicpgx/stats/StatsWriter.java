package org.cpicpgx.stats;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.stats.model.StatisticType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Class to take Statistics from the {@link AbstractStatistics} classes and write the results to the database.
 */
public class StatsWriter implements AutoCloseable {

  private final Date f_timestamp = new Date();
  private final Connection f_conn;
  private final PreparedStatement f_insert;

  public StatsWriter() throws SQLException {
    f_conn = ConnectionFactory.newConnection();
    f_insert = f_conn
        .prepareStatement("insert into cpic.statistic(createdon, stattype, statvalue) values (?, ?, ?)");
  }

  public int write(Map<StatisticType, Long> statMap) throws SQLException {
    int count = 0;
    for (StatisticType type : statMap.keySet()) {
      count += write(type, statMap.get(type));
    }
    return count;
  }

  public int write(StatisticType type, long stat) throws SQLException {
    f_insert.setTimestamp(1, new Timestamp(f_timestamp.getTime()));
    f_insert.setString(2, type.name());
    f_insert.setLong(3, stat);
    return f_insert.executeUpdate();
  }

  @Override
  public void close() throws Exception {
    if (f_conn != null) {
      f_conn.close();
    }
  }
}
