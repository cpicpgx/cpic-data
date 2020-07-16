package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
  private static final Pattern sf_activityScorePattern = Pattern.compile("^[≥>]?\\d+\\.?\\d*$");
  static final String NA = "n/a";
  static final String EXCEL_EXTENSION = ".xlsx";

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

  /**
   * Gets the type of Note for this importer based on {@link BaseDirectoryImporter#getFileType()}
   * @return a {@link NoteType} enum value
   * @throws RuntimeException when notes are not supported
   */
  NoteType getNoteType() {
    switch (getFileType()) {
      case ALLELE_DEFINITION:
        return NoteType.ALLELE_DEFINITION;
      case FREQUENCIES:
        return NoteType.ALLELE_FREQUENCY;
      case GENE_CDS:
        return NoteType.CDS;
      case ALLELE_FUNCTION_REFERENCE:
        return NoteType.FUNCTION_REFERENCE;
      case TEST_ALERTS:
        return NoteType.TEST_ALERT;
      case RECOMMENDATIONS:
        return NoteType.RECOMMENDATIONS;
      default:
        throw new RuntimeException("Notes are not supported");
    }
  }
  
  abstract String[] getDeleteStatements();
  
  abstract String getDefaultDirectoryName();

  /**
   * Calling this method 
   */
  public void clearAllData() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      int delCount = 0;
      for (String deleteStmt : getDeleteStatements()) {
        try (PreparedStatement stmt = conn.prepareStatement(deleteStmt)) {
          delCount += stmt.executeUpdate();
        }
      }
      if (getDeleteStatements().length > 0) {
        sf_logger.info("Deleted {} rows for {}", delCount, getFileType().name());
      }
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

  static String makeFunctionKey(String f1, String f2, String as1, String as2) {
    JsonObject key = new JsonObject();
    if (f1 != null) {
      if (f1.equals(f2)) {
        key.addProperty(f1, 2);
      } else {
        key.addProperty(f1, 1);
        key.addProperty(f2, 1);
      }
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

  /**
   * Writes the supplied list of notes to the DB for the given type/id pair
   * @param entityType the type of object to note, either gene or drug
   * @param entityId the ID of the object to note
   * @param notes a List of String notes
   * @throws SQLException can occur from DB inserts
   */
  void writeNotes(EntityType entityType, String entityId, List<String> notes) throws SQLException {
    if (notes == null || notes.size() == 0) return;

    String insertSqlTemplate = "insert into %s(%s, note, type, ordinal) values (?, ?, ?, ?)";
    String insertSql;
    switch (entityType) {
      case GENE:
        insertSql = String.format(insertSqlTemplate, "gene_note", "genesymbol");
        break;
      case DRUG:
        insertSql = String.format(insertSqlTemplate, "drug_note", "drugid");
        break;
      default:
        throw new RuntimeException(String.format("Type not supported: %s", entityType.name()));
    }

    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement noteInsert = conn.prepareStatement(insertSql);
      int n = 0;
      for (String note : notes) {
        noteInsert.setString(1, entityId);
        noteInsert.setString(2, note);
        noteInsert.setString(3, getNoteType().name());
        noteInsert.setInt(4, n);
        noteInsert.executeUpdate();
        n += 1;
      }
      sf_logger.debug("created {} new notes", notes.size());
    }
  }

  /**
   * Normalize activity score Strings before they can be inserted into the DB. Null values are allowed since not all
   * genes use activity scores. Normalize the strings to strip trailing ".0" so all sources will agree. Also, blank
   * Strings will return as null.
   *
   * @param score an optionally null score string
   * @return a normalized score string
   */
  @Nullable
  static String normalizeScore(@Nullable String score) {
    if (StringUtils.isBlank(score)) {
      return null;
    } else {
      if (score.toLowerCase().equals(NA)) {
        return NA;
      }
      else if (sf_activityScorePattern.matcher(score).matches()) {
        return score.replaceAll("\\.0$", "");
      } else {
        throw new RuntimeException("Activity score not in expected format: " + score);
      }
    }
  }
}
