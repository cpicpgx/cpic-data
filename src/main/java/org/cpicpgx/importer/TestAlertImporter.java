package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.DbLookup;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An importer for CDS test-alert text from excel workbooks into the database.
 * 
 * This importer is split into two different importers for two different formats of test alerts. One for a single CDS 
 * trigger, another for a double CDS trigger. The drugs that use double trigger need to be specified here.
 * 
 * A single trigger format expects columns in the following order
 * <ol>
 *   <li>CDS Trigger</li>
 *   <li>Diagram reference point (optional)</li>
 *   <li>Context</li>
 *   <li>Example alert text</li>
 *   <li>Second Example alert text (optional)</li>
 * </ol>
 * 
 * A double trigger format expects columns
 * <ol>
 *   <li>Trigger 1</li>
 *   <li>Trigger 2</li>
 *   <li>Diagram reference point</li>
 *   <li>Context</li>
 *   <li>Example alert text</li>
 * </ol>
 *
 * @author Ryan Whaley
 */
public class TestAlertImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  // Drugs to 
  private static final List<String> sf_twoTriggerDrugs = new ArrayList<>();
  static {
    sf_twoTriggerDrugs.add("RxNorm:704");   // amitriptyline
    sf_twoTriggerDrugs.add("RxNorm:1256");  // azathioprine
    sf_twoTriggerDrugs.add("RxNorm:2002");  // carbamazepine
    sf_twoTriggerDrugs.add("RxNorm:2597");  // clomipramine
    sf_twoTriggerDrugs.add("RxNorm:3638");  // doxepin
    sf_twoTriggerDrugs.add("RxNorm:5691");  // imipramine
    sf_twoTriggerDrugs.add("RxNorm:103");   // mercaptopurine
    sf_twoTriggerDrugs.add("RxNorm:10485"); // thioguanine
    sf_twoTriggerDrugs.add("RxNorm:10834"); // trimipramine
  }
  private static final String[] sf_deleteStatements = new String[]{
      "delete from test_alerts"
  };
  private static final String DEFAULT_DIRECTORY = "test_alerts";

  public static void main(String[] args) {
    rebuild(new TestAlertImporter(), args);
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
    PreparedStatement insert = conn.prepareStatement(
        "insert into test_alerts(cds_context, trigger_condition, drugid, alert_text, reference_point, activity_score) values (?, ?, ?, ?, ?, ?)");
    int contextIdx = -1;
    List<Integer> triggerIdxs = new ArrayList<>();
    Map<Integer,String> alertMap = new LinkedHashMap<>();
    int refPointIdx = -1;
    int activityIdx = -1;
    
    RowWrapper headerRow = workbook.getRow(0);
    for (int i=headerRow.row.getFirstCellNum(); i<headerRow.row.getLastCellNum(); i++) {
      String colTitle = headerRow.getNullableText(i);
      if (colTitle == null) continue;
      
      colTitle = colTitle.toLowerCase();
      if (colTitle.contains("flow chart")) {
        refPointIdx = i;
      } else if (colTitle.contains("trigger")) {
        triggerIdxs.add(i);
      } else if (colTitle.contains("activity")) {
        activityIdx = i;
      } else if (colTitle.contains("cds context")) {
        contextIdx = i;
      } else if (colTitle.contains("alert text")) {
        alertMap.put(i, headerRow.getNullableText(i));
      }
    }
    
    for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
      sf_logger.debug("reading row {}", i);

      RowWrapper row = workbook.getRow(i);
      if (row.hasNoText(contextIdx)) {
        continue;
      }

      String context = row.getNullableText(contextIdx);

      List<String> triggers = new ArrayList<>();
      if (triggerIdxs.size() > 0) {
        triggerIdxs.forEach((ti) -> triggers.add(row.getNullableText(ti)));
      }

      insert.setString(1, context);
      insert.setArray(2, conn.createArrayOf("text", triggers.toArray()));
      insert.setString(3, drugId);

      if (alertMap.size()>0) {
        List<String> alertValues = new ArrayList<>();
        for (Integer alertIdx : alertMap.keySet()) {
          if (alertMap.get(alertIdx).equals("CDS Alert Text")) {
            alertValues.add(row.getNullableText(alertIdx));
          } else {
            alertValues.add(alertMap.get(alertIdx) + ": " + row.getNullableText(alertIdx));
          }
        }
        Array alertArg = conn.createArrayOf("text", alertValues.toArray());
        insert.setArray(4, alertArg);
      } else {
        insert.setNull(4, Types.ARRAY);
      }

      if (refPointIdx >= 0) {
        insert.setString(5, row.getNullableText(refPointIdx, true));
      } else {
        insert.setNull(5, Types.VARCHAR);
      }
      
      if (activityIdx >= 0) {
        insert.setString(6, row.getNullableText(activityIdx));
      } else {
        insert.setNull(6, Types.VARCHAR);
      }

      insert.executeUpdate();
    }
  }
}
