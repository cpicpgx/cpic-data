package org.cpicpgx;

import org.apache.commons.cli.*;
import org.cpicpgx.exporter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.IllegalPathStateException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an archive of data file artifacts generated from the CPIC database. This will write to a 
 * specified directory with subdirectories for the different types of data. This relies on {@link BaseExporter} classes 
 * that will write out batches of files.
 * 
 * This will write to a directory with a pre-determined name that uses the current date in the name.
 *
 * @author Ryan Whaley
 */
public class DataArtifactArchive {
  
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String sf_dirNamePattern = "cpic_information";

  private Path m_baseDirectory;
  private boolean upload = false;

  public static void main(String[] args) {
    try {
      DataArtifactArchive dataArtifactArchive = new DataArtifactArchive();
      dataArtifactArchive.parseArgs(args);
      dataArtifactArchive.write();
    } catch (Exception e) {
      sf_logger.error("Error making gene data artifact", e);
    }
  }
  
  private void parseArgs(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", true,"path to directory to write files to");
    options.addOption("u", false, "flag to upload generated files to FileStore (S3)");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cli = clParser.parse(options, args);

    m_baseDirectory = Paths.get(cli.getOptionValue("d"));
    upload = cli.hasOption("u");
  }
  
  private void write() {
    if (m_baseDirectory == null) {
      throw new IllegalStateException("No path to directory specified");
    }
    if (!m_baseDirectory.toFile().exists() || !m_baseDirectory.toFile().isDirectory()) {
      throw new IllegalPathStateException("Not a directory: " + m_baseDirectory);
    }
    
    List<BaseExporter> exporters = new ArrayList<>();
    exporters.add(new AlleleDefinitionExporter());
    exporters.add(new AlleleFunctionalityReferenceExporter());
    exporters.add(new FrequencyExporter());
    exporters.add(new DiplotypePhenotypeExporter());
    exporters.add(new GeneCdsExporter());
    exporters.add(new GeneResourceExporter());
    exporters.add(new PhenotypesExporter());
    exporters.add(new DrugResourceExporter());
    exporters.add(new RecommendationExporter());
    exporters.add(new TestAlertExporter());
    exporters.add(new PairsExporter());

    exporters.forEach(e -> {
      Path dirPath = getDirectoryPath(sf_dirNamePattern + "/" + e.getFileType().name().toLowerCase());
      e.setDirectory(dirPath);
      try {
        e.setUpload(upload);
        e.export();
      } catch (Exception ex) {
        throw new RuntimeException("Error exporting " + e.getClass().getSimpleName(), ex);
      }
    });
  }
  
  private Path getDirectoryPath(String filePath) {
    Path dir = m_baseDirectory.resolve(filePath);
    if (dir.toFile().mkdirs()) {
      sf_logger.info("Created new directory {}", dir);
    } else {
      sf_logger.info("Using existing directory {}", dir);
    }
    return dir;
  }
}
