package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.workbook.AlleleSummaryWorkbook;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * A class to query all current alleles and dump them out to a summary sheet
 *
 * @author Ryan Whaley
 */
public class AlleleSummaryExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    AlleleSummaryExporter exporter = new AlleleSummaryExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting allele summary", ex);
    }
  }

  @Override
  public FileType getFileType() {
    return FileType.ALLELE_SUMMARY;
  }

  @Override
  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement stmt = conn.prepareStatement("select genesymbol as Gene, allele_name as Allele, guideline_name as Guideline, guideline_url as URL from allele_guideline_view order by genesymbol, allele_name, guideline_name");
        ResultSet rs = stmt.executeQuery()
    ) {
      AlleleSummaryWorkbook workbook = new AlleleSummaryWorkbook();
      while (rs.next()) {
        workbook.writeRow(
            rs.getString(1),
            rs.getString(2),
            rs.getString(3),
            rs.getString(4)
        );
      }
      writeWorkbook(workbook);
      addFileExportHistory(workbook.getFilename(), new String[]{});
      handleFileUpload();
    }
  }
}
