package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.DbLookup;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * An importer for drug CDS test-alert text from excel workbooks into the database.
 * 
 * @author Ryan Whaley
 */
public class TestAlertImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String[] sf_deleteStatements = new String[]{
      "delete from drug_note where type='" + NoteType.TEST_ALERT.name() + "'",
      "delete from test_alerts"
  };
  private static final String FILE_EXTENSION = "_Pre_and_Post_Test_Alerts.xlsx";
  private static final String DEFAULT_DIRECTORY = "test_alerts";

  public static void main(String[] args) {
    rebuild(new TestAlertImporter(), args);
  }
  
  public TestAlertImporter() {}

  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.TEST_ALERTS;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return FILE_EXTENSION;
  }
  
  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    for (Iterator<Sheet> sheetIterator = workbook.getSheetIterator(); sheetIterator.hasNext();) {
      Sheet sheet = sheetIterator.next();
      String population = sheet.getSheetName().replaceFirst("population ", "");
      try (Connection conn = ConnectionFactory.newConnection()) {
        processTwoTrigger(workbook, conn, population);
      }
    }
  }

  private static final int COL_DRUG = 0;
  private static final int COL_TRIGGER_START = 1;
  private static final String COL_NAME_CONTEXT = "CDS Context, Relative to Genetic Testing";
  private static final String COL_NAME_ALERT = "CDS Alert Text";

  private void processTwoTrigger(WorkbookWrapper workbook, Connection conn, String population) throws Exception {
    PreparedStatement insert = conn.prepareStatement(
        "insert into test_alerts(cds_context, trigger_condition, drugid, alert_text, population) values (?, ?, ?, ?, ?)");
    PreparedStatement insertNote = conn.prepareStatement(
        "insert into drug_note(drugId, type, ordinal, note) values (?, ?, ?, ?)");
    DrugCache drugCache = new DrugCache(conn);

    RowWrapper headerRow = workbook.getRow(0);
    Map<String, Integer> triggerNames = new HashMap<>();
    int idxContext = -1;
    int idxAlert = -1;
    for (int h=COL_TRIGGER_START; h<= headerRow.row.getLastCellNum(); h++) {
      String triggerName = StringUtils.strip(headerRow.getNullableText(h));
      if (StringUtils.isBlank(triggerName)) {
        continue;
      } else if (triggerName.equals(COL_NAME_CONTEXT)) {
        idxContext = h;
      } else if (triggerName.equals(COL_NAME_ALERT)) {
        idxAlert = h;
      } else {
        triggerNames.put(triggerName, h);
      }
    }

    List<String> notes = new ArrayList<>();
    boolean noteMode = false;
    for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
      sf_logger.debug("reading row {}", i);
      RowWrapper row = workbook.getRow(i);

      // no text at either the beginning or the end, skip it
      if (row.hasNoText(COL_DRUG) && row.hasNoText(idxAlert)) continue;

      String firstValue = row.getNullableText(COL_DRUG);
      if (firstValue.equalsIgnoreCase("notes")) {
        noteMode = true;
        continue;
      }

      // if there's only text in the first column, it must be a note
      if (noteMode) {
        if (row.getNullableText(0) != null) {
          notes.add(row.getNullableText(0));
        }
        continue;
      }

      List<String> triggers = new ArrayList<>();
      for (String triggerName : triggerNames.keySet()) {
        triggers.add(triggerName + " = " + row.getNullableText(triggerNames.get(triggerName)));
      }
      String drugId = drugCache.lookup(row.getNullableText(COL_DRUG));
      Array triggerSqlArray = conn.createArrayOf("VARCHAR", triggers.toArray());
      String context = row.getNullableText(idxContext);
      Array alertSqlArray = conn.createArrayOf("VARCHAR", new String[]{row.getNullableText(idxAlert)});

      insert.clearParameters();
      insert.setString(1, context);
      insert.setArray(2, triggerSqlArray);
      insert.setString(3, drugId);
      insert.setArray(4, alertSqlArray);
      insert.setString(5, population);
      insert.executeUpdate();
    }

    for (int i = 0; i < notes.size(); i++) {
      String note = notes.get(i);
      for (String drugId : drugCache.getIds()) {
        insertNote.setString(1, drugId);
        insertNote.setString(2, NoteType.TEST_ALERT.name());
        insertNote.setInt(3, i);
        insertNote.setString(4, note);
        insertNote.executeUpdate();
      }
    }
  }

  private static class DrugCache {
    private Connection conn;
    private Map<String, String> nameToIdMap = new HashMap<>();

    private DrugCache(Connection conn) {
      this.conn = conn;
    }

    private String lookup(String name) throws Exception {
      if (StringUtils.isBlank(name)) {
        return null;
      }

      String id = nameToIdMap.get(name);
      if (id == null) {
        id = DbLookup.getDrugByName(conn, name)
                .orElseThrow(() -> new NotFoundException("No drug for " + name));
        nameToIdMap.put(name, id);
      }
      return id;
    }

    private Collection<String> getIds() {
      return this.nameToIdMap.values();
    }
  }
}
