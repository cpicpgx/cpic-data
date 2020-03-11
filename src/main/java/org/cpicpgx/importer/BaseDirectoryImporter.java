package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.FileHistoryWriter;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Abstract class for classes that want to crawl all files in a directory and do something with them. This should be 
 * extensible enough for any file type but it's mostly used for parsing Excel files and CSVs.
 * 
 * For an Excel parser example, check out {@link DiplotypePhenotypeImporter}.
 * 
 * For a CSV example, check out {@link RecommendationImporter}
 *
 * @author Ryan Whaley
 */
public abstract class BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static final String EXCEL_EXTENSION = ".xlsx";
  static final String CSV_EXTENSION = ".csv";
  
  private Path directory;

  /**
   * Gets the String file extension to look for in the given directory. This should be something like ".xlsx" or ".csv".
   * @return a file extension to filter for
   */
  abstract String getFileExtensionToProcess();

  /**
   * Gets the type of file this importer is importing
   * @return a {@link FileType} enum value
   */
  abstract FileType getFileType();
  
  abstract String[] getDeleteStatements();
  
  abstract String getDefaultDirectoryName();

  /**
   * Calling this method 
   */
  public void clearAllData() throws SQLException {
    Connection conn = ConnectionFactory.newConnection();

    int delCount = 0;
    for (String deleteStmt : getDeleteStatements()) {
      try (PreparedStatement stmt = conn.prepareStatement(deleteStmt)) {
        delCount += stmt.executeUpdate();
      }
    }
    if (getDeleteStatements().length > 0) {
      sf_logger.info("Deleted {} rows", delCount);
    }
  }
  
  static void rebuild(BaseDirectoryImporter importer, String[] args) {
    try {
      importer.parseArgs(args);
      importer.clearAllData();
      importer.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    } catch (SQLException e) {
      sf_logger.error("Error from database", e);
    }
  }

  /**
   * Parse arguments from the command line.
   * 
   * Expect a "d" argument that gives the directory to crawl through. Must exist and must contain files.
   * @param args an array of command line arguments
   * @throws ParseException can occur from bad argument syntax
   */
  private void parseArgs(String [] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", true,"directory containing files to process (*.xlsx)");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cli = clParser.parse(options, args);

    String directoryPath = cli.getOptionValue("d");
    setDirectory(Paths.get(directoryPath));
  }

  /**
   * Run the importer. Requires the "directory" to be set
   */
  public void execute() {
    Arrays.stream(Objects.requireNonNull(this.directory.toFile().listFiles()))
        .filter(f -> f.getName().toLowerCase().endsWith(getFileExtensionToProcess().toLowerCase()) && !f.getName().startsWith("~$"))
        .forEach(getFileProcessor());
  }

  /**
   * A {@link Consumer} that will take a {@link File} objects and then run {@link BaseDirectoryImporter#processWorkbook(WorkbookWrapper)}
   * on them. You either need to override {@link BaseDirectoryImporter#processWorkbook(WorkbookWrapper)} or override 
   * this method to do something different with the {@link File} 
   * @return a Consumer of File objects
   */
  Consumer<File> getFileProcessor() {
    return (File file) -> {
      sf_logger.info("Reading {}", file);

      try (InputStream in = Files.newInputStream(file.toPath())) {
        WorkbookWrapper workbook = new WorkbookWrapper(in);
        workbook.setFileName(file.getName());
        processWorkbook(workbook);
      } catch (Exception ex) {
        throw new RuntimeException("Error processing file " + file, ex);
      }
    };
  }

  /**
   * This method is meant to pass in a parsed {@link WorkbookWrapper} object and then do something with it. This must 
   * be overriden and will throw an error if it is not.
   * @param workbook a workbook pulled from the specified directory
   * @throws Exception will be thrown if this method is not overridden
   */
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    throw new RuntimeException("Workbook processor not implemented in this importer");
  }

  public BaseDirectoryImporter setDirectory(Path parentDir, String dir) {
    setDirectory(parentDir.resolve(StringUtils.defaultIfBlank(dir, getDefaultDirectoryName())));
    return this;
  }

  /**
   * Sets a directory to search for files to process. The path must exist and be for a directory (not a file)
   * @param directory a directory in the filesystem
   */
  private void setDirectory(Path directory) {
    this.directory = directory;

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

  /**
   * This will add a message to the history for a file with a default message saying the data was imported at the 
   * current time
   * @param fileName the file to log was imported
   * @throws SQLException can occur from DB interaction
   */
  void addImportHistory(String fileName) throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      FileHistoryWriter fileHistoryWriter = new FileHistoryWriter(conn, getFileType());
      fileHistoryWriter.write(fileName, "imported to database from file");
    }
  }

  void addImportHistory(WorkbookWrapper workbook) throws SQLException {
    addImportHistory(workbook.getFileName());
  }

  static String makeFunctionKey(String f1, String f2, String as1, String as2) {
    JsonObject key = new JsonObject();
    if (f1.equals(f2)) {
      key.addProperty(f1, 2);
    } else {
      key.addProperty(f1, 1);
      key.addProperty(f2, 1);
    }
    if (as1 != null) {
      if (as1.equals(as2)) {
        key.addProperty(as1, 2);
      } else {
        key.addProperty(as1, 1);
        key.addProperty(as2, 1);
      }
    }
    return key.toString();
  }
}
