package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

  EntityType getEntityCategory() {
    return EntityType.DRUG;
  }

  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select d.name, t.drugid, max(array_length(t.trigger_condition,1)) as num_alerts from test_alerts t join drug d on t.drugid = d.drugid group by d.name, t.drugid");
         PreparedStatement alertStmt = conn.prepareStatement("select t.trigger_condition, t.reference_point, t.cds_context, t.alert_text from test_alerts t where t.drugid=?");
         PreparedStatement noteStmt = conn.prepareStatement("select note from drug_note where drugId=? order by ordinal");
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
                ars.getString(3),
                (String[])ars.getArray(4).getArray());
            alertCount++;
          }
          sf_logger.info("Wrote {} alerts", alertCount);
        }
        
        noteStmt.clearParameters();
        noteStmt.setString(1, drugId);
        List<String> notes = new ArrayList<>();
        try (ResultSet rsNote = noteStmt.executeQuery()) {
          while (rsNote.next()) {
            notes.add(rsNote.getString(1));
          }
        }
        workbook.writeNotes(notes);

        writeWorkbook(workbook);
      }
    }
    handleFileUpload();
  }
}
