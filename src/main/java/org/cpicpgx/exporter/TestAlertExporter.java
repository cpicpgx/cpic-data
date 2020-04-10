package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * An exporter for writing test alerts to an excel sheet grouped by drug.
 *
 * @author Ryan Whaley
 */
public class TestAlertExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    TestAlertExporter exporter = new TestAlertExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting test alerts", ex);
    }
  }

  FileType getFileType() {
    return FileType.TEST_ALERTS;
  }

  EntityType getEntityCategory() {
    return EntityType.DRUG;
  }

  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select d.name, t.drugid, max(array_length(t.trigger_condition,1)) as num_alerts from test_alerts t join drug d on t.drugid = d.drugid group by d.name, t.drugid");
         PreparedStatement alertStmt = conn.prepareStatement("select t.trigger_condition, t.cds_context, t.alert_text from test_alerts t where t.drugid=?");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String drugName = grs.getString(1);
        String drugId = grs.getString(2);
        int triggerCount = grs.getInt(3);
        sf_logger.info("Writing {}", drugName);

        TestAlertWorkbook workbook = new TestAlertWorkbook(drugName, triggerCount);
        alertStmt.setString(1, drugId);
        
        try (ResultSet ars = alertStmt.executeQuery()) {
          int alertCount = 0;
          while (ars.next()) {
            workbook.writeAlert(
                (String[])ars.getArray(1).getArray(),
                ars.getString(2),
                (String[])ars.getArray(3).getArray());
            alertCount++;
          }
          sf_logger.info("Wrote {} alerts", alertCount);
        }
        
        workbook.writeNotes(queryDrugNotes(conn, drugId, NoteType.TEST_ALERT));

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{drugId});
      }
      handleFileUpload();
    }
  }
}
