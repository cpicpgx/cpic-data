package org.cpicpgx.stats.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class for representing GitHub Assets in the GitHub API response
 */
public class GitHubAsset {
  @JsonProperty("name")
  private String m_name;
  @JsonProperty("download_count")
  private Integer m_downloadCount;
  @JsonProperty("size")
  private Integer m_size;

  public String getName() {
    return m_name;
  }

  public void setName(String name) {
    m_name = name;
  }

  public Integer getDownloadCount() {
    return m_downloadCount;
  }

  public void setDownloadCount(Integer downloadCount) {
    m_downloadCount = downloadCount;
  }

  public Integer getSize() {
    return m_size;
  }

  public void setSize(Integer size) {
    m_size = size;
  }
}
