package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An importer for drug CDS test-alert text from excel workbooks into the database.
 * 
 * @author Ryan Whaley
 */
public class TestAlertImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String[] sf_deleteStatements = new String[]{
      "delete from drug_note where type='" + NoteType.TEST_ALERT.name() + "'",
      "delete from test_alert"
  };
  private static final Pattern sf_activityScorePattern = Pattern.compile("^(.+)?[Aa]ctivity [Ss]core$");
  private static final Pattern sf_phenotypePattern = Pattern.compile("^(.+)?[Pp]henotype$");

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
      if (!sheet.getSheetName().startsWith("population ")) {
        throw new RuntimeException("Sheet name does not start with \"population\": " + sheet.getSheetName());
      }

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
        "insert into test_alert(cds_context, genes, drugid, alert_text, population, activity_score, phenotype) values (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)");
    PreparedStatement insertNote = conn.prepareStatement(
        "insert into drug_note(drugId, type, ordinal, note) values (?, ?, ?, ?)");
    DrugCache drugCache = new DrugCache(conn);

    RowWrapper headerRow = workbook.getRow(0);

    Map<String, Integer> activityCols = new TreeMap<>();
    Map<String, Integer> phenotypeCols = new TreeMap<>();

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
        Matcher activityMatch = sf_activityScorePattern.matcher(triggerName);
        Matcher phenotypeMatch = sf_phenotypePattern.matcher(triggerName);
        if (activityMatch.matches()) {
          String activityGene = StringUtils.stripToNull(activityMatch.group(1));
          if (activityGene == null) {
            throw new RuntimeException("No gene specified in the activity column " + (h + 1));
          }
          activityCols.put(StringUtils.strip(activityMatch.group(1)), h);
        } else if (phenotypeMatch.matches()) {
          phenotypeCols.put(StringUtils.strip(phenotypeMatch.group(1)), h);
        } else {
          throw new RuntimeException("Trigger column not in expected format: " + triggerName);
        }
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

      JsonObject activityJson = new JsonObject();
      for (String gene : activityCols.keySet()) {
        activityJson.addProperty(gene, row.getText(activityCols.get(gene)));
      }

      JsonObject phenotypeJson = new JsonObject();
      for (String gene : phenotypeCols.keySet()) {
        String phenotype = WordUtils.capitalize(row.getText(phenotypeCols.get(gene)).replaceAll(gene + " ", ""));
        phenotypeJson.addProperty(gene, phenotype);
      }

      String drugId = drugCache.lookup(row.getNullableText(COL_DRUG));
      Array geneArray = conn.createArrayOf("VARCHAR", activityCols.keySet().toArray());
      String context = StringUtils.replace(row.getNullableText(idxContext), "Test", "test");
      Array alertSqlArray = conn.createArrayOf("VARCHAR", new String[]{row.getNullableText(idxAlert)});

      insert.clearParameters();
      insert.setString(1, context);
      insert.setArray(2, geneArray);
      insert.setString(3, drugId);
      insert.setArray(4, alertSqlArray);
      insert.setString(5, population);
      insert.setString(6, activityJson.toString());
      //noinspection JpaQueryApiInspection
      insert.setString(7, phenotypeJson.toString());
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
    private final Connection conn;
    private final Map<String, String> nameToIdMap = new HashMap<>();

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
