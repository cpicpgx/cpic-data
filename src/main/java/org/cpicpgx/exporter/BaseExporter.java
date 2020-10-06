package org.cpicpgx.exporter;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.FileHistoryWriter;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.FileStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A base class for handling the basics of what a exporter class will need
 *
 * @author Ryan Whaley
 */
public abstract class BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected Path directory;
  private boolean upload = false;
  private final List<Path> generatedFiles = new ArrayList<>();

  /**
   * The type of file this exports
   * @return a {@link FileType}
   */
  public abstract FileType getFileType();

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
    options.addOption("u", false, "upload to S3 bucket");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cli = clParser.parse(options, args);

    String directoryPath = cli.getOptionValue("d");
    setDirectory(directoryPath);

    upload = cli.hasOption("u");
  }

  /**
   * Set the directory based on a String of the path to it
   * @param directory a path to an existing directory
   */
  private void setDirectory(String directory) {
    if (StringUtils.stripToNull(directory) == null) {
      throw new IllegalArgumentException("Directory not specified");
    }
    
    this.directory = Paths.get(directory);

    if (!this.directory.toFile().exists()) {
      if (!this.directory.toFile().mkdirs()) {
        throw new IllegalArgumentException("Directory doesn't exist " + this.directory);
      }
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
  
  public void setUpload(boolean upload) {
    this.upload = upload;
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
    generatedFiles.add(filePath);
    sf_logger.info("Wrote {}", filePath);
  }

  /**
   * This method will check if user wants to upload and then upload the generated files to S3 and put them in the 
   * proper directory if the user has flagged that they want upload.
   */
  void handleFileUpload() {
    if (!upload) {
      return;
    }
    try (FileStoreClient fileStore = new FileStoreClient()) {
      generatedFiles.forEach(f -> fileStore.putArtifact(f, getFileType()));
    }
  }

  /**
   * Get the notes for a given entity and note type
   * @param conn an open Database connection
   * @param entityId a gene symbol, drug ID, or other entity identifier
   * @param type the note type to query for
   * @return an ordered list of notes
   * @throws SQLException can occur from database query
   */
  List<String> queryNotes(Connection conn, String entityId, FileType type) throws SQLException {
    List<String> notes = new ArrayList<>();
    try (
        PreparedStatement noteStmt = conn.prepareStatement(
            "select n.note from file_note n where entityId=? and type=? order by ordinal"
        )
    ) {
      noteStmt.setString(1, entityId);
      noteStmt.setString(2, type.name());
      try (ResultSet rs = noteStmt.executeQuery()) {
        while (rs.next()) {
          notes.add(rs.getString(1));
        }
      }
    }
    return notes;
  }

  /**
   * Gets a List of change log events as an Object array. The first element of the array is a {@link java.util.Date} of
   * the log event and the second element is the {@link String} note.
   * @param conn an open database connection
   * @param entityId a gene symbol or drug ID
   * @param type the type of change log event
   * @return a List of 2-element Object arrays
   * @throws SQLException can occur from DB interactions
   */
  List<Object[]> queryChangeLog(Connection conn, String entityId, FileType type) throws SQLException {
    List<Object[]> changeLog = new ArrayList<>();
    try (
        PreparedStatement logStmt = conn.prepareStatement("select date, note from change_log where entityid=? and type=? order by date desc")
        ) {
      logStmt.setString(1, entityId);
      logStmt.setString(2, type.name());
      try (ResultSet rs = logStmt.executeQuery()) {
        while (rs.next()) {
          changeLog.add(new Object[]{rs.getDate(1), rs.getString(2)});
        }
      }
    }
    return changeLog;
  }

  void addFileExportHistory(String fileName, String[] entityIds) throws Exception {
    try (FileHistoryWriter historyWriter = new FileHistoryWriter(getFileType())) {
      historyWriter.writeExport(fileName, entityIds);
    }
  }
}
