package org.cpicpgx.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

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
  private static final String S3_GENERIC_KEY_PREFIX = "data/";
  private static final String S3_GENE_KEY_PREFIX    = "data/gene/";
  private static final String S3_DRUG_KEY_PREFIX    = "data/drug/";
  
  private AmazonS3 s3;
  
  public FileStoreClient() {
    s3 = AmazonS3ClientBuilder.defaultClient();
  }
  
  public void putGeneArtifact(Path geneFilePath) {
    putFile(S3_GENE_KEY_PREFIX, geneFilePath.getFileName().toString(), geneFilePath.toFile());
  }
  
  public void putDrugArtifact(Path drugFilePath) {
    putFile(S3_DRUG_KEY_PREFIX, drugFilePath.getFileName().toString(), drugFilePath.toFile());
  }
  
  public void putArtifact(Path filePath) {
    putFile(S3_GENERIC_KEY_PREFIX, filePath.getFileName().toString(), filePath.toFile());
  }
  
  private void putFile(String prefix, String fileName, File geneFile) {
    s3.putObject(S3_PUBLIC_BUCKET, prefix + fileName, geneFile);
    sf_logger.info("Uploaded {}", String.format("s3:///%s/%s%s", S3_PUBLIC_BUCKET, prefix, fileName));
  }
  
  @Override
  public void close() {
    if (s3 != null) {
      s3.shutdown();
    }
  }
}
