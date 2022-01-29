package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.workbook.DrugResourceWorkbook;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;

/**
 * A class to export files, by drug, of the external IDs used to identify that drug
 *
 * @author Ryan Whaley
 */
public class DrugResourceExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    DrugResourceExporter exporter = new DrugResourceExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting drug resource", ex);
    }
  }

  @Override
  public FileType getFileType() {
    return FileType.DRUG_RESOURCE;
  }

  @Override
  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement drugStmt = conn.prepareStatement("select drugid, name, pharmgkbid, rxnormid, drugbankid, atcid, umlscui from drug d");
        ResultSet rs = drugStmt.executeQuery()
        ) {
      while (rs.next()) {
        DrugResourceWorkbook workbook = new DrugResourceWorkbook(rs.getString(2));

        String[] atcCodes = new String[0];
        if (rs.getArray(6) != null) {
          atcCodes = (String[])rs.getArray(6).getArray();
        }

        workbook.writeMapping(
            rs.getString(4),
            rs.getString(5),
            atcCodes,
            rs.getString(3)
        );

        workbook.writeChangeLog(Collections.emptyList());

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{rs.getString(1)});
      }
      handleFileUpload();
    }
  }
}
