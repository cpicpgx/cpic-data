package org.cpicpgx.exporter;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class to handle exporting a CPIC report out to filesystem.
 *
 * @author Ryan Whaley
 */
public class PairsExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String sf_pairQuery = "select " +
      "       genesymbol as \"Gene\", " +
      "       drugname as \"Drug\", " +
      "       guidelineurl as \"Guideline URL\", " +
      "       cpiclevel as \"CPIC Level\", " +
      "       pgkbcalevel as \"PharmGKB Level of Evidence\", " +
      "       pgxtesting as \"PGx Level of Evidence\", " +
      "       array_to_string(pmids, ';') as \"CPIC Publications (PMID)\" " +
      "from pair_view order by cpiclevel, drugname, genesymbol";
  //language=PostgreSQL
  private static final String sf_changeLogQuery = "select date, note from change_log where type=?";

  public static void main(String[] args) {
    try {
      PairsExporter export = new PairsExporter();
      export.parseArgs(args);
      export.export();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }

  public FileType getFileType() {
    return FileType.PAIR;
  }

  EntityType getEntityCategory() {
    return EntityType.PAIR;
  }

  public void export() {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement stmt = conn.prepareStatement(sf_pairQuery);
        PreparedStatement changeStmt = conn.prepareStatement(sf_changeLogQuery)
    ) {
      PairWorkbook pairWorkbook = new PairWorkbook();
      pairWorkbook.writeHeader(stmt.getMetaData());
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          pairWorkbook.writePair(rs);
        }
      }

      pairWorkbook.writeHistoryHeader();
      changeStmt.setString(1, FileType.PAIR.name());
      try (ResultSet rs = changeStmt.executeQuery()) {
        while (rs.next()) {
          pairWorkbook.writeHistory(rs.getDate(1), rs.getString(2));
        }
      }

      addFileExportHistory(pairWorkbook.getFilename(), new String[]{});

      writeWorkbook(pairWorkbook);
    } catch (IOException e) {
      sf_logger.error("Couldn't write to filesystem", e);
    } catch (SQLException e) {
      sf_logger.error("Couldn't query the DB", e);
    } catch (Exception e) {
      sf_logger.error("Error making pairs", e);
    }
  }
}
