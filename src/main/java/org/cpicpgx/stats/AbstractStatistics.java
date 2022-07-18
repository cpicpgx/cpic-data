package org.cpicpgx.stats;

import org.cpicpgx.stats.model.StatisticType;

import java.util.Map;

/**
 * All statistics gatherers must implement a <code>gather</code> method
 */
public interface AbstractStatistics {
  public Map<StatisticType, Long> gather();
}
