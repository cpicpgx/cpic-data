package org.cpicpgx.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.net.UrlEscapers;
import org.cpicpgx.db.FileHistoryWriter;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class that helps write files to the network file store, Amazon S3.
 * 
 * This class assumes you have <a href="https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html">AWS credentials setup on your system</a>.
 * This usually entails setting up a <code>~/.aws</code> directory with configuration and credential files but can be 
 * handled in other ways if necessary.
 *
 * @author Ryan Whaley
 */
public class FileStoreClient implements AutoCloseable {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private static final String S3_PUBLIC_BUCKET      = "files.cpicpgx.org";
  private static final String S3_GENERIC_KEY_PREFIX = "data/report/";
  private static final String S3_GUIDELINE_STAGING_FORMAT = "data/guideline/staging/%s/";
  private static final String S3_URL_FORMAT         = "https://" + S3_PUBLIC_BUCKET + "/%s%s";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  
  private final AmazonS3 s3;
  private FileHistoryWriter fileHistoryWriter;
  
  public FileStoreClient() {
    s3 = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.US_WEST_2)
            .build();
    try {
      fileHistoryWriter = new FileHistoryWriter(null);
    } catch (SQLException e) {
      sf_logger.error("Could not start DB connection", e);
    }
  }

  /**
   * Build URL to S3 resource while properly escaping special characters in the URL.
   * @return a properly escaped URL (e.g. spaces are now "%20")
   */
  public static String buildS3Url(String path, String fileName) {
    return String.format(
        S3_URL_FORMAT,
        path,
        UrlEscapers.urlPathSegmentEscaper().escape(fileName)
    );
  }
  
  public void putArtifact(Path filePath, FileType type) {
    String fileName = filePath.getFileName().toString();

    String datedDirPath = S3_GENERIC_KEY_PREFIX + DATE_FORMAT.format(new Date()) + "/" + type.name().toLowerCase() + "/";
    putFile(datedDirPath, fileName, filePath.toFile());

    String currentDirPath = S3_GENERIC_KEY_PREFIX + "current/" + type.name().toLowerCase() + "/";
    putFile(currentDirPath, fileName, filePath.toFile());

    try {
      fileHistoryWriter.writeUpload(fileName, buildS3Url(datedDirPath, fileName));
    } catch (SQLException e) {
      sf_logger.error("Error updating file record in DB " + fileName, e);
    }
  }

  public String putGuidelineStagingFile(Path filePath, String timestamp) {
    String fileName = filePath.getFileName().toString();
    String prefix = String.format(S3_GUIDELINE_STAGING_FORMAT, timestamp);
    putFile(prefix, fileName, filePath.toFile());
    return escapeUrl(buildS3Url(prefix, fileName));
  }

  private void putFile(String directoryPath, String fileName, File file) {
    s3.putObject(S3_PUBLIC_BUCKET, directoryPath + fileName, file);
    sf_logger.info("Uploaded {}", String.format("s3:///%s/%s%s", S3_PUBLIC_BUCKET, directoryPath, fileName));
  }

  public static String escapeUrl(String rawUrl) {
    try {
      URL parsedUrl = new URL(rawUrl);
      URI parsedUri = new URI(parsedUrl.getProtocol(),
          null,
          parsedUrl.getHost(),
          parsedUrl.getPort(),
          parsedUrl.getPath(),
          parsedUrl.getQuery(),
          parsedUrl.getRef());
      return parsedUri.toURL().toString();
    } catch (MalformedURLException|URISyntaxException e) {
      throw new RuntimeException("Error escaping URL text", e);
    }
  }
  
  @Override
  public void close() {
    if (s3 != null) {
      s3.shutdown();
    }
    if (fileHistoryWriter != null) {
      try {
        fileHistoryWriter.close();
      } catch (Exception e) {
        sf_logger.error("Could not close DB connection", e);
      }
    }
  }
}
