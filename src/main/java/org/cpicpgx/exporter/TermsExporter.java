package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Exporter that queries the DB for standard terms and then writes them to an Excel sheet
 *
 * @author Ryan Whaley
 */
public class TermsExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    TermsExporter exporter = new TermsExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting recommendations", ex);
    }
  }

  public FileType getFileType() {
    return FileType.TERM;
  }

  EntityType getEntityCategory() {
    return EntityType.TERM;
  }

  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement drugStmt = conn.prepareStatement("select category, functionaldef, geneticdef, term.term from term");
        ResultSet rs = drugStmt.executeQuery()
    ) {
      TermsWorkbook workbook = new TermsWorkbook();
      while (rs.next()) {
        workbook.writeTerm(
            rs.getString(1),
            rs.getString(2),
            rs.getString(3),
            rs.getString(4)
        );
      }
      writeWorkbook(workbook);
      handleFileUpload();
      addFileExportHistory(workbook.getFilename(), new String[]{});
    }
  }
}
