package org.cpicpgx.stats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cpicpgx.stats.model.GitHubRelease;
import org.cpicpgx.stats.model.StatisticType;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that gathers stats from the GitHub API for the cpic-data repo
 */
public class GitHubStatistics implements AbstractStatistics {
  private static final String GITHUB_RELEASES = "https://api.github.com/repos/cpicpgx/cpic-data/releases";

  private final List<GitHubRelease> f_releases;

  GitHubStatistics() throws IOException {
    ObjectMapper jsonMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    f_releases = jsonMapper.readValue(
        new URL(GITHUB_RELEASES),
        new TypeReference<List<GitHubRelease>>(){});
  }

  @Override
  public Map<StatisticType, Long> gather() {
    Map<StatisticType, Long> stats = new HashMap<>();
    stats.put(StatisticType.COUNT_ALL_RELEASE, (long)getReleases().size());
    stats.put(StatisticType.COUNT_RELEASE, getReleaseCount());
    stats.put(StatisticType.COUNT_DB_DL, getDownloadCount());
    stats.put(StatisticType.SIZE_DB_TRANSFER, getDownloadSize());
    return stats;
  }

  public List<GitHubRelease> getReleases() {
    return f_releases;
  }

  public long getReleaseCount() {
    return getReleases().stream()
        .filter(r -> !r.isPrerelease())
        .count();
  }

  public Date getMaxPublished() {
    return getReleases().stream()
        .map(GitHubRelease::getPublished)
        .max(Date::compareTo)
        .orElse(null);
  }

  public Date getReleasePublished() {
    return getReleases().stream()
        .filter(r -> !r.isPrerelease())
        .map(GitHubRelease::getPublished)
        .min(Date::compareTo)
        .orElse(null);
  }

  public Date getPrereleasePublished() {
    return getReleases().stream()
        .map(GitHubRelease::getPublished)
        .min(Date::compareTo)
        .orElse(null);
  }

  public long getDownloadSize() {
    return getReleases().stream()
        .mapToInt(GitHubRelease::getTotalDownloadSize)
        .sum();
  }

  public long getDownloadCount() {
    return getReleases().stream()
        .mapToInt(GitHubRelease::getTotalDownloadCount)
        .sum();
  }
}
