package org.cpicpgx.stats.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

/**
 * Model class for representing a GitHub Release entry in the GitHub API response
 */
public class GitHubRelease {

  @JsonProperty("tag_name")
  private String m_tagName;
  @JsonProperty("name")
  private String m_name;
  @JsonProperty("assets")
  private List<GitHubAsset> m_assets;
  @JsonProperty("published_at")
  private Date m_published;
  @JsonProperty("prerelease")
  private boolean m_prerelease;

  public String getTagName() {
    return m_tagName;
  }

  public void setTagName(String tagName) {
    m_tagName = tagName;
  }

  public String getName() {
    return m_name;
  }

  public void setName(String name) {
    m_name = name;
  }

  public List<GitHubAsset> getAssets() {
    return m_assets;
  }

  public void setAssets(List<GitHubAsset> assets) {
    m_assets = assets;
  }

  public int getTotalDownloadCount() {
    return getAssets().stream()
        .filter(a -> a.getName().endsWith(".sql.gz"))
        .mapToInt(GitHubAsset::getDownloadCount)
        .sum();
  }

  public int getTotalDownloadSize() {
    return getAssets().stream()
        .filter(a -> a.getName().endsWith(".sql.gz"))
        .mapToInt(GitHubAsset::getSize)
        .sum();
  }

  public Date getPublished() {
    return m_published;
  }

  public void setPublished(Date published) {
    m_published = published;
  }

  public boolean isPrerelease() {
    return m_prerelease;
  }

  public void setPrerelease(boolean prerelease) {
    m_prerelease = prerelease;
  }
}
