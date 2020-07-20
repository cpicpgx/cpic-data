package org.cpicpgx.exporter;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.FileHistoryWriter;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.model.EntityType;
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
  private List<Path> generatedFiles = new ArrayList<>();

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
  
  public void setUpload(boolean upload) {
    this.upload = upload;
  }

  /**
   * Gets the type of entity this exporter is related to. For example, if this export mainly deals with information 
   * related to genes then you would choose {@link EntityType#GENE}
   * @return the entity type this export relates to
   */
  abstract EntityType getEntityCategory();
  
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
      switch (getEntityCategory()) {
        case GENE:
          generatedFiles.forEach(fileStore::putGeneArtifact);
          break;
        case DRUG:
          generatedFiles.forEach(fileStore::putDrugArtifact);
          break;
        default:
          generatedFiles.forEach(fileStore::putArtifact);
      }
      
    }
  }

  /**
   * Get the notes for a given gene and note type
   * @param conn an open Database connection
   * @param symbol a gene symbol
   * @param type the note type to query for
   * @return an ordered list of notes
   * @throws SQLException can occur from database query
   */
  List<String> queryGeneNotes(Connection conn, String symbol, NoteType type) throws SQLException {
    List<String> notes = new ArrayList<>();
    try (
        PreparedStatement noteStmt = conn.prepareStatement(
            "select n.note from gene_note n where genesymbol=? and type=? and n.date is null order by ordinal"
        )
    ) {
      noteStmt.setString(1, symbol);
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
   * Get the notes for a given drug and note type
   * @param conn an open Database connection
   * @param drugId the ID of the drug to query for
   * @param type the note type to query for
   * @return an ordered list of notes
   * @throws SQLException can occur from database query
   */
  List<String> queryDrugNotes(Connection conn, String drugId, NoteType type) throws SQLException {
    List<String> notes = new ArrayList<>();
    try (
        PreparedStatement noteStmt = conn.prepareStatement(
            "select n.note from drug_note n where drugid=? and type=? and n.date is null order by ordinal"
        )
    ) {
      noteStmt.setString(1, drugId);
      noteStmt.setString(2, type.name());
      try (ResultSet rs = noteStmt.executeQuery()) {
        while (rs.next()) {
          notes.add(rs.getString(1));
        }
      }
    }
    return notes;
  }

  void addFileExportHistory(String fileName, String[] entityIds) throws Exception {
    try (FileHistoryWriter historyWriter = new FileHistoryWriter(getFileType())) {
      historyWriter.writeExport(fileName, entityIds);
    }
  }
}
