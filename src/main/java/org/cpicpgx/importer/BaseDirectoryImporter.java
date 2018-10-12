package org.cpicpgx.importer;

import org.apache.commons.cli.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Abstract class for classes that want to crawl all files in a directory and do something with them
 *
 * @author Ryan Whaley
 */
abstract class BaseDirectoryImporter {
  
  private Path directory;

  /**
   * Parse arguments from the command line.
   * 
   * Expect a "d" argument that gives the directory to crawl through. Must exist and must contain files.
   * @param args an array of command line arguments
   * @throws ParseException
   */
  void parseArgs(String [] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", true,"directory containing allele frequency excel files (*.xlsx)");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cli = clParser.parse(options, args);

    String directoryPath = cli.getOptionValue("d");
    setDirectory(directoryPath);
  }

  /**
   * A predicate for filtering the files in a directory based on their file extension
   * @param fileExt a file extension like ".csv"
   * @return a Predicate filter
   */
  Predicate<File> filterFileFunction(String fileExt) {
    return f -> f.getName().toLowerCase().endsWith(fileExt.toLowerCase()) && !f.getName().startsWith("~$");
  }

  /**
   * Gets a stream of all the files in the directory
   */
  Stream<File> streamFiles() {
    return Arrays.stream(Objects.requireNonNull(this.directory.toFile().listFiles()));
  }

  /**
   * Sets the directory to work with. Will fail if the directory doesn't exist or doesn't have files
   * @param directory a directory path
   */
  void setDirectory(String directory) {
    if (directory == null) {
      throw new IllegalArgumentException("Need a directory");
    }
    
    this.directory = Paths.get(directory);

    if (!this.directory.toFile().exists()) {
      throw new IllegalArgumentException("Directory doesn't exist " + this.directory);
    }
    if (!this.directory.toFile().isDirectory()) {
      throw new IllegalArgumentException("Path is not a directory " + this.directory);
    }
    if (this.directory.toFile().listFiles() == null) {
      throw new IllegalArgumentException("Directory is empty " + this.directory);
    }
  }
}
