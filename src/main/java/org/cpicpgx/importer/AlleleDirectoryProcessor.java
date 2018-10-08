package org.cpicpgx.importer;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class crawls the given directory for <code>.xlsx</code> files and runs the {@link AlleleDefinitionImporter} on 
 * each one in succession.
 *
 * @author Ryan Whaley
 */
public class AlleleDirectoryProcessor {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Path m_directory;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("d", true,"directory containing allele definition excel files (*.xlsx)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      AlleleDirectoryProcessor processor = new AlleleDirectoryProcessor(Paths.get(cli.getOptionValue("d")));
      processor.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }
  
  public AlleleDirectoryProcessor(Path directoryPath) {
    if (directoryPath == null) {
      throw new IllegalArgumentException("No directory given");
    }
    
    if (!directoryPath.toFile().exists()) {
      throw new IllegalArgumentException("Directory doesn't exist " + directoryPath);
    }
    if (!directoryPath.toFile().isDirectory()) {
      throw new IllegalArgumentException("Path is not a directory " + directoryPath);
    }
    if (directoryPath.toFile().listFiles() == null) {
      throw new IllegalArgumentException("Directory is empty " + directoryPath);
    }
    
    m_directory = directoryPath;
  }
  
  public void execute() {
    Arrays.stream(Objects.requireNonNull(m_directory.toFile().listFiles()))
        .filter(f -> f.getName().toLowerCase().endsWith(".xlsx") && !f.getName().startsWith("~$"))
        .forEach(f -> {
          try {
            AlleleDefinitionImporter importer = new AlleleDefinitionImporter(f.getAbsolutePath());
            importer.writeToDB();
            sf_logger.info("Processed {}", f);
          } catch (SQLException e) {
            throw new RuntimeException("Error processing " + f, e);
          }
        });
  }
}
