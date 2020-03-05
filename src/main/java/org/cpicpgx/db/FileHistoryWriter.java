package org.cpicpgx.db;

import org.cpicpgx.model.FileType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class that helps with writing history messages to the database for file artifacts either read or written by this code 
 *
 * @author Ryan Whaley
 */
public class FileHistoryWriter {
  // for messages from this code
  public static final String SOURCE_SYSTEM="SYSTEM";
  // for messages that come from input files
  public static final String SOURCE_FILE="FILE";
  
  private PreparedStatement insertFile;
  private PreparedStatement insertHistory;
  private FileType fileType;
  
  public FileHistoryWriter(Connection conn, FileType fileType) throws SQLException {
    insertFile = conn.prepareStatement("insert into file_artifact(type, fileName) values (?, ?) on conflict (fileName) do nothing returning id");
    insertHistory = conn.prepareStatement("insert into file_artifact_history(fileId, changeMessage, source) values (?, ?, ?)");
    this.fileType = fileType;
  }

  /**
   * Find the primary ID for a file or create a new one if this file hasn't been seen before
   * @param fileName a String file name
   * @return the long ID of the file
   * @throws SQLException can occur from DB activity
   */
  private long lookupFileId(String fileName) throws SQLException {
    this.insertFile.clearParameters();
    this.insertFile.setString(1, this.fileType.name());
    this.insertFile.setString(2, fileName);
    try (ResultSet insertResult = this.insertFile.executeQuery()) {
      insertResult.next();
      return insertResult.getLong(1);
    }
  }

  /**
   * Writes a timestamped message associated with the given file name to the database. This method will source the 
   * message to "SYSTEM" meaning it's automated and not human-curated
   * @param fileName a String file name
   * @param message the message to write for this file
   * @throws SQLException can occur from DB activity
   */
  public void write(String fileName, String message) throws SQLException {
    write(fileName, message, SOURCE_SYSTEM);
  }

  /**
   * Writes a timestamped message associated with the given file name to the database and specifies where the source of 
   * this message is from (use {@link FileHistoryWriter#SOURCE_SYSTEM} or {@link FileHistoryWriter#SOURCE_FILE})
   * @param fileName a String file name
   * @param message the message to write for this file
   * @param source the source to attribute this message to
   * @throws SQLException can occur from DB activity
   */
  public void write(String fileName, String message, String source) throws SQLException {
    long fileId = lookupFileId(fileName);
    this.insertHistory.setLong(1, fileId);
    this.insertHistory.setString(2, message);
    this.insertHistory.setString(3, source);
    this.insertHistory.executeUpdate();
  }
}
