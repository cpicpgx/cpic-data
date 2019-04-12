package org.cpicpgx.exporter;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A base class for handling the basics of what a exporter class will need
 *
 * @author Ryan Whaley
 */
public abstract class BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  protected Path directory;

  /**
   * Parse arguments from the command line.
   *
   * Expect a "d" argument that gives the directory to write to. Must exist.
   * @param args an array of command line arguments
   * @throws ParseException can occur from bad argument syntax
   */
  void parseArgs(String [] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", true,"directory to write files to");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cli = clParser.parse(options, args);

    String directoryPath = cli.getOptionValue("d");
    setDirectory(directoryPath);
  }

  /**
   * Set the directory based on a String of the path to it
   * @param directory a path to an existing directory
   */
  void setDirectory(String directory) {
    if (StringUtils.stripToNull(directory) == null) {
      throw new IllegalArgumentException("Directory not specified");
    }
    
    this.directory = Paths.get(directory);

    if (!this.directory.toFile().exists()) {
      throw new IllegalArgumentException("Directory doesn't exist " + this.directory);
    }
    if (!this.directory.toFile().isDirectory()) {
      throw new IllegalArgumentException("Path is not a directory " + this.directory);
    }
  }

  /**
   * Set a directory to write to
   * @param directory a directory to write files to
   */
  public void setDirectory(Path directory) {
    this.directory = directory;
  }

  /**
   * The method that will export files
   * @throws Exception can occur from querying the DB
   */
  public abstract void export() throws Exception;
  
  void writeWorkbook(AbstractWorkbook workbook) throws IOException {
    workbook.getSheets().forEach(SheetWrapper::autosizeColumns);

    Path filePath = this.directory.resolve(workbook.getFilename());
    try (OutputStream out = Files.newOutputStream(filePath)) {
      workbook.write(out);
    }
    sf_logger.info("Wrote {}", filePath);
  }
}
