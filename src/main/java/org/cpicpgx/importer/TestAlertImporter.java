package org.cpicpgx.importer;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.DbLookup;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * An importer for CDS test-alert text from excel workbooks into the database
 *
 * @author Ryan Whaley
 */
public class TestAlertImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      TestAlertImporter importer = new TestAlertImporter();
      importer.parseArgs(args);
      importer.execute();
    } catch (ParseException e) {
      sf_logger.error("Error parsing args", e);
    }
  }
  
  private TestAlertImporter() {}

  public TestAlertImporter(Path directory) {
    this.setDirectory(directory);
  }
  
  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }
  
  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement insert = conn.prepareStatement(
          "insert into test_alerts(cds_context, trigger_condition, drugid, reference_point, alert_text) values (?, ?, ?, ?, ?)");

      String drugName = workbook.currentSheet.getSheetName();
      String drugId = DbLookup.getDrugByName(conn, drugName)
          .orElseThrow(() -> new NotFoundException("No drug for " + drugName));

      String trigger1 = "";
      String trigger2 = "";
      String refPoint = "";
      String context = "";
      List<String> alerts = new ArrayList<>();
      for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
        sf_logger.debug("reading row {}", i);
        
        RowWrapper row = workbook.getRow(i);
        if (
            StringUtils.isNotBlank(trigger1) && StringUtils.isNotBlank(trigger2) // ensure first row doesn't trigger
            && (
                (row.hasNoText(0) && row.hasNoText(1)) // insert at end of rows
                || (!trigger1.equals(row.getNullableText(0)) && !trigger2.equals(row.getNullableText(1))) // insert on change in triggers
            )
        ) {
          Array triggers = conn.createArrayOf("text", new String[]{trigger1, trigger2});
          Array alertArg = conn.createArrayOf("text", alerts.toArray());

          insert.setString(1, context);
          insert.setArray(2, triggers);
          insert.setString(3, drugId);
          insert.setString(4, refPoint);
          insert.setArray(5, alertArg);

          insert.executeUpdate();
        }
        
        if (row.hasNoText(4)) break; // alert should always have text
        
        trigger1 = StringUtils.defaultIfBlank(row.getNullableText(0), trigger1);
        trigger2 = StringUtils.defaultIfBlank(row.getNullableText(1), trigger2);
        refPoint = StringUtils.defaultIfBlank(row.getNullableText(2, true), refPoint);
        context = StringUtils.defaultIfBlank(row.getNullableText(3), context);
        if (row.getNullableText(4) != null) {
          alerts.add(row.getNullableText(4));
        }
      }
    }
  }
}
