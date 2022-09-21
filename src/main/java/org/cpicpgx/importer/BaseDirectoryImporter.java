package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.workbook.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
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
  private static final Pattern sf_activityScorePattern = Pattern.compile("^[≥>]?\\d+(\\.?\\d*)$");
  private static final Pattern sf_noResultPattern = Pattern.compile("^No (.*)Result$");

  private Path directory;

  /**
   * Gets the String file extension to look for in the given directory. This should be something like ".xlsx" or ".csv".
   *
   * @return a file extension to filter for
   */
  abstract String getFileExtensionToProcess();

  /**
   * Gets the type of file this importer is importing
   *
   * @return a {@link FileType} enum value
   */
  abstract FileType getFileType();

  abstract String[] getDeleteStatements();

  /**
   * Gets the default directory name for the data type. Should rely on {@link FileType}
   *
   * @return a default directory name, all lowercase
   */
  String getDefaultDirectoryName() {
    return getFileType().name().toLowerCase();
  }

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
   * <p>
   * Expect a "d" argument that gives the directory to crawl through. Must exist and must contain files.
   *
   * @param args an array of command line arguments
   * @throws ParseException can occur from bad argument syntax
   */
  private void parseArgs(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", true, "directory containing files to process (*.xlsx)");
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
        .sorted()
        .forEach(getFileProcessor());
  }

  /**
   * A {@link Consumer} that will take a {@link File} objects and then run {@link BaseDirectoryImporter#processWorkbook(WorkbookWrapper)}
   * on them. You either need to override {@link BaseDirectoryImporter#processWorkbook(WorkbookWrapper)} or override
   * this method to do something different with the {@link File}
   *
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
   *
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
   *
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
   * Creates the lookup key used for linking recommendations and test alerts to gene phenotypes
   *
   * @param f1  the first allele function
   * @param f2  the second allele function, possibly n/a
   * @param as1 the first activity value
   * @param as2 the second activity value, possibly n/a
   * @return a string to match the lookup key between recommendation and phenotype
   */
  static String makeLookupKey(String f1, String f2, String as1, String as2) {
    JsonObject key = new JsonObject();
    if (f1 != null) {
      if (f1.equals(f2)) {
        key.addProperty(f1, 2);
      } else {
        key.addProperty(f1, 1);
        if (!f2.equalsIgnoreCase(Constants.NA)) {
          key.addProperty(f2, 1);
        }
      }
    }
    if (as1 != null) {
      if (as1.equals(as2)) {
        key.addProperty(as1, 2);
      } else {
        key.addProperty(as1, 1);
        if (!as2.equalsIgnoreCase(Constants.NA)) {
          key.addProperty(as2, 1);
        }
      }
    }
    return key.toString();
  }

  /**
   * Writes the supplied list of notes to the DB for the given id
   *
   * @param entityId the ID of the object to note
   * @param notes    a List of String notes
   * @throws SQLException can occur from DB inserts
   */
  void writeNotes(String entityId, List<String> notes) throws SQLException {
    if (notes == null || notes.size() == 0) return;

    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement noteInsert = conn.prepareStatement("insert into file_note(entityId, note, type, ordinal) values (?, ?, ?, ?)");
      int n = 0;
      for (String note : notes) {
        noteInsert.setString(1, entityId);
        noteInsert.setString(2, note);
        noteInsert.setString(3, getFileType().name());
        noteInsert.setInt(4, n);
        noteInsert.executeUpdate();
        n += 1;
      }
      sf_logger.debug("created {} new notes", notes.size());
    }
  }

  /**
   * Normalize activity score Strings before they can be inserted into the DB. Null values are allowed since not all
   * genes use activity scores. Normalize the strings to add trailing ".0" so all sources will agree. Also, blank
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
      Matcher m = sf_activityScorePattern.matcher(score);
      if (score.equalsIgnoreCase(Constants.NA)) {
        return Constants.NA;
      } else if (Constants.isNoResult(score)) {
        return Constants.NO_RESULT;
      } else if (m.matches()) {
        if (StringUtils.isBlank(m.group(1))) {
          return score + ".0";
        }
        return score;
      } else {
        throw new RuntimeException("Activity score not in expected format: " + score);
      }
    }
  }

  /**
   * Normalize text by taking out the gene name and stripping extraneous whitespace.
   *
   * @param gene the gene text to be removed
   * @param text the text to normalize
   * @return normalized version of input "text", possibly null
   */
  @Nullable
  static String normalizeGeneText(@Nonnull String gene, @Nullable String text) {
    String strippedText = StringUtils.stripToNull(text);
    if (strippedText == null) {
      return null;
    } else if (strippedText.equalsIgnoreCase(Constants.NA)) {
      return Constants.NA;
    } else {
      String normalName = strippedText.replaceAll(gene + "\\s*", "");

      Matcher m = sf_noResultPattern.matcher(normalName);
      if (m.matches()) {
        if (StringUtils.isNotBlank(m.group(1))) {
          throw new RuntimeException("No Result value [" + text + "] should not include text [" + m.group(1) + "]");
        }
      }

      return normalName;
    }
  }

  /**
   * Normalize text for activity score, will replace nulls and blanks with "n/a"
   *
   * @param rawText raw text to describe an activity score
   * @return a normalized score or "n/a" if no value specified
   */
  static String normalizeActivityScore(@Nullable String rawText) {
    String text = StringUtils.stripToNull(rawText);
    if (text == null || Constants.isUnspecified(text)) {
      return Constants.NA;
    } else {
      return text.replaceAll(">=", "≥").replaceAll("\\.0$", "");
    }
  }

  void processChangeLog(DbHarness db, WorkbookWrapper workbook, @Nullable String entityId) throws SQLException {
    for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
      RowWrapper row = workbook.getRow(i);
      if (row.hasNoText(0) ^ row.hasNoText(1)) {
        throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
      } else if (row.hasNoText(0)) continue;

      java.util.Date date = row.getDate(0);
      String note = row.getText(1);
      db.writeChangeLog(entityId, date, note);
    }
  }

  String processMethods(WorkbookWrapper workbook) throws SQLException {
    StringJoiner methodsText = new StringJoiner("\n");
    try {
      workbook.findSheet(AbstractWorkbook.METHODS_SHEET_PATTERN);
      for (int i = 0; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        if (row.hasNoText(0)) {
          methodsText.add("");
        } else {
          methodsText.add(StringUtils.defaultIfBlank(row.getNullableText(0), ""));
        }
      }
    } catch (NotFoundException ex) {
      sf_logger.debug("No methods sheet found");
    }
    return methodsText.toString();
  }
}
