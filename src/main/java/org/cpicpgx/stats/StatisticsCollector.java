package org.cpicpgx.stats;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Collects a snapshot of statistics from various sources and saves it to the database.
 *
 * This is safe to run multiple times but each time will create a new set of statistics (with the same timestamp).
 */
public class StatisticsCollector {

  public static void main(String[] args) {

    try (StatsWriter writer = new StatsWriter()) {
      List<AbstractStatistics> allStats = ImmutableList.of(
          new DatabaseStatistics(),
          new GitHubStatistics()
      );
      for (AbstractStatistics s : allStats) {
        writer.write(s.gather());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }
}
