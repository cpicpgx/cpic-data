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
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * An importer for CDS test-alert text from excel workbooks into the database
 *
 * @author Ryan Whaley
 */
public class TestAlertImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final List<String> sf_twoTriggerDrugs = new ArrayList<>();
  static {
    sf_twoTriggerDrugs.add("RxNorm:2002");
    sf_twoTriggerDrugs.add("RxNorm:704");
  }
  private static final String[] sf_deleteStatements = new String[]{
      "delete from test_alerts"
  };
  private static final String DEFAULT_DIRECTORY = "test_alerts";

  public static void main(String[] args) {
    try {
      TestAlertImporter importer = new TestAlertImporter();
      importer.parseArgs(args);
      importer.execute();
    } catch (ParseException e) {
      sf_logger.error("Error parsing args", e);
    }
  }
  
  public TestAlertImporter() {}

  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }
  
  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    try (Connection conn = ConnectionFactory.newConnection()) {
      String drugName = workbook.currentSheet.getSheetName();
      String drugId = DbLookup.getDrugByName(conn, drugName)
          .orElseThrow(() -> new NotFoundException("No drug for " + drugName));
      
      if (sf_twoTriggerDrugs.contains(drugId)) {
        processTwoTrigger(workbook, conn, drugId);
      } else {
        processOneTrigger(workbook, conn, drugId);
      }
    }
  }

  private void processTwoTrigger(WorkbookWrapper workbook, Connection conn, String drugId) throws SQLException {
    PreparedStatement insert = conn.prepareStatement(
        "insert into test_alerts(cds_context, trigger_condition, drugid, reference_point, alert_text) values (?, ?, ?, ?, ?)");

    String lastTrigger1 = null;
    String lastTrigger2 = null;
    String lastRefPoint = null;
    String lastContext = null;
    List<String> alerts = new ArrayList<>();
    for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
      sf_logger.debug("reading row {}", i);
      RowWrapper row = workbook.getRow(i);
      if (row.row == null) break;

      String currentTrigger1 = row.getNullableText(0);
      String currentTrigger2 = row.getNullableText(1);
      String currentRefPoint = row.getNullableText(2, true);
      String currentContext = row.getNullableText(3);
      String currentAlert = row.getNullableText(4);

      if (
          !(lastTrigger1 == null && lastTrigger2 == null)
          && (currentTrigger1 != null || currentTrigger2 != null)
      ) {
        Array triggers = conn.createArrayOf("text", new String[]{lastTrigger1, lastTrigger2});
        Array alertArg = conn.createArrayOf("text", alerts.toArray());

        insert.setString(1, lastContext);
        insert.setArray(2, triggers);
        insert.setString(3, drugId);
        insert.setString(4, lastRefPoint);
        insert.setArray(5, alertArg);

        insert.executeUpdate();
        alerts.clear();
      }

      if (row.hasNoText(4)) break; // alert should always have text

      lastTrigger1 = StringUtils.defaultIfBlank(currentTrigger1, lastTrigger1);
      lastTrigger2 = StringUtils.defaultIfBlank(currentTrigger2, lastTrigger2);
      lastRefPoint = StringUtils.defaultIfBlank(currentRefPoint, lastRefPoint);
      lastContext = StringUtils.defaultIfBlank(currentContext, lastContext);
      if (currentAlert != null) {
        alerts.add(currentAlert);
      } else {
        break;
      }
    }
  }

  private void processOneTrigger(WorkbookWrapper workbook, Connection conn, String drugId) throws SQLException {
    PreparedStatement insert;
    int contextIdx;
    int triggerIdx;
    int alertIdx;
    int refPointIdx = -1;
    
    RowWrapper headerRow = workbook.getRow(2);
    if (headerRow.getNullableText(1).contains("Flow Chart")) {
      insert = conn.prepareStatement(
          "insert into test_alerts(cds_context, trigger_condition, drugid, alert_text, reference_point) values (?, ?, ?, ?, ?)");
      contextIdx = 2;
      triggerIdx = 0;
      alertIdx = 3;
      refPointIdx = 1;
    } else {
      insert = conn.prepareStatement(
          "insert into test_alerts(cds_context, trigger_condition, drugid, alert_text) values (?, ?, ?, ?)");
      contextIdx = 0;
      triggerIdx = 1;
      alertIdx = 2;
    }

    for (int i = 3; i <= workbook.currentSheet.getLastRowNum(); i++) {
      sf_logger.debug("reading row {}", i);

      RowWrapper row = workbook.getRow(i);
      if (row.hasNoText(2)) break; // alert should always have text

      String context = row.getNullableText(contextIdx);
      String trigger = row.getNullableText(triggerIdx);
      String alert = row.getNullableText(alertIdx);

      Array triggers = conn.createArrayOf("text", new String[]{trigger});
      Array alertArg = conn.createArrayOf("text", new String[]{alert});

      insert.setString(1, context);
      insert.setArray(2, triggers);
      insert.setString(3, drugId);
      insert.setArray(4, alertArg);
      
      if (refPointIdx > 0) {
        insert.setString(5, row.getNullableText(refPointIdx, true));
      }

      insert.executeUpdate();
    }
  }
}
