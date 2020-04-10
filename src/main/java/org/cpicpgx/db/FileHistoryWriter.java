package org.cpicpgx.db;

import org.cpicpgx.model.FileType;

import java.sql.*;

/**
 * Class that helps with writing history messages to the database for file artifacts either read or written by this code 
 *
 * @author Ryan Whaley
 */
public class FileHistoryWriter implements AutoCloseable {
  // for messages from this code
  public static final String SOURCE_SYSTEM="SYSTEM";

  private static final String DEFAULT_EXPORT_MESSAGE = "exported from DB";

  private Connection connection;
  private PreparedStatement insertFile;
  private PreparedStatement insertHistory;
  private PreparedStatement updateUrl;
  private FileType fileType;
  
  public FileHistoryWriter(FileType fileType) throws SQLException {
    this.connection = ConnectionFactory.newConnection();
    insertFile = this.connection.prepareStatement("insert into file_artifact(type, fileName, entityIds) values (?, ?, ?) on conflict (fileName) do update set entityids=excluded.entityids");
    insertHistory = this.connection.prepareStatement("insert into file_artifact_history(fileId, changeMessage, source) select id, ?, ? from file_artifact where filename=?");
    updateUrl = this.connection.prepareStatement("update file_artifact set url=? where fileName=?");
    this.fileType = fileType;
  }

  /**
   * Makes a record for the given file name. It's ok to run on the same file multiple times, it will only create one record.
   * @param fileName a String file name
   * @throws SQLException can occur from DB activity
   */
  private void makeFile(String fileName, String[] entityIds) throws SQLException {
    this.insertFile.clearParameters();
    this.insertFile.setString(1, this.fileType.name());
    this.insertFile.setString(2, fileName);
    if (entityIds != null && entityIds.length > 0) {
      this.insertFile.setArray(3, this.connection.createArrayOf("TEXT", entityIds));
    } else {
      this.insertFile.setNull(3, Types.ARRAY);
    }
    this.insertFile.executeUpdate();
  }

  /**
   * Writes a timestamped message associated with the given file name to the database. This method will source the 
   * message to "SYSTEM" meaning it's automated and not human-curated
   * @param fileName a String file name
   * @throws SQLException can occur from DB activity
   */
  public void writeExport(String fileName, String[] entityIds) throws SQLException {
    write(fileName, DEFAULT_EXPORT_MESSAGE, SOURCE_SYSTEM, entityIds);
  }

  public void writeUpload(String fileName, String url) throws SQLException {
    this.updateUrl.clearParameters();
    this.updateUrl.setString(1, url);
    this.updateUrl.setString(2, fileName);
    this.updateUrl.executeUpdate();
  }

  /**
   * Writes a timestamped message associated with the given file name to the database and specifies where the source of 
   * this message is from (use {@link FileHistoryWriter#SOURCE_SYSTEM})
   * @param fileName a String file name
   * @param message the message to write for this file
   * @param source the source to attribute this message to
   * @throws SQLException can occur from DB activity
   */
  private void write(String fileName, String message, String source, String[] entityIds) throws SQLException {
    makeFile(fileName, entityIds);
    this.insertHistory.setString(1, message);
    this.insertHistory.setString(2, source);
    this.insertHistory.setString(3, fileName);
    this.insertHistory.executeUpdate();
  }

  @Override
  public void close() throws Exception {
    if (this.insertFile != null) {
      this.insertFile.close();
    }
    if (this.insertHistory != null) {
      this.insertHistory.close();
    }
    if (this.updateUrl != null) {
      this.updateUrl.close();
    }
    if (this.connection != null) {
      this.connection.close();
    }
  }
}
