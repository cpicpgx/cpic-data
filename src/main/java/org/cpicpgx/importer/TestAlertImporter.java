package org.cpicpgx.importer;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.DbLookup;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.*;
import java.util.Date;
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

  private final Set<String> f_notes = new LinkedHashSet<>();

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
    try (DbHarness dbHarness = new DbHarness()) {
      for (Iterator<Sheet> sheetIterator = workbook.getSheetIterator(); sheetIterator.hasNext(); ) {
        Sheet sheet = sheetIterator.next();

        if (AbstractWorkbook.HISTORY_SHEET_NAME.equalsIgnoreCase(sheet.getSheetName())) {
          workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
          processHistory(workbook, dbHarness);
        }
        else if (!sheet.getSheetName().equalsIgnoreCase("flow chart")) {
          String population = sheet.getSheetName().replaceFirst("population ", "");
          processTestAlertSheet(workbook, dbHarness, population);
        }
      }
      for (String note : f_notes) {
        dbHarness.writeNote(note);
      }
      f_notes.clear();
    }
  }

  private static final int COL_DRUG = 0;
  private static final int COL_TRIGGER_START = 1;
  private static final String COL_NAME_CONTEXT = "CDS Context, Relative to Genetic Testing";
  private static final String COL_NAME_ALERT = "CDS Alert Text";

  private void processTestAlertSheet(WorkbookWrapper workbook, DbHarness dbHarness, String population) throws Exception {
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

    if (activityCols.size() != phenotypeCols.size()) {
      sf_logger.warn("activity and phenotype columns do not match");
    }
    if (idxContext == -1) {
      sf_logger.warn("no context column found");
    }
    if (idxAlert == -1) {
      sf_logger.warn("no alert text column found");
    }

    dbHarness.addGenes(activityCols.keySet());

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
        if (!row.hasNoText(0)) {
          f_notes.add(row.getText(0));
        }
        continue;
      }

      Map<String, String> activityJson = new HashMap<>();
      for (String gene : activityCols.keySet()) {
        activityJson.put(gene, row.getText(activityCols.get(gene)));
      }

      Map<String,String> phenotypeJson = new HashMap<>();
      for (String gene : phenotypeCols.keySet()) {
        String phenotype = WordUtils.capitalize(row.getText(phenotypeCols.get(gene)).replaceAll(gene + " ", ""));
        phenotypeJson.put(gene, phenotype);
      }

      String context = StringUtils.replace(row.getText(idxContext), "Test", "test");

      dbHarness.writeAlert(context, activityCols.keySet(), row.getText(COL_DRUG), row.getText(idxAlert), population, activityJson, phenotypeJson);
    }
  }

  void processHistory(WorkbookWrapper workbook, DbHarness dbHarness) throws SQLException {
    for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
      RowWrapper row = workbook.getRow(i);
      if (row.hasNoText(0) ^ row.hasNoText(1)) {
        throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
      } else if (row.hasNoText(0)) continue;

      java.util.Date date = row.getDate(0);
      String note = row.getText(1);
      dbHarness.writeHistory(date, note, i - 1);
    }
  }

  private class DbHarness implements AutoCloseable {
    private final Connection conn;
    private final PreparedStatement insert;
    private final PreparedStatement insertNote;
    private final PreparedStatement insertChangeStmt;
    private final PreparedStatement findLookup;
    private final PreparedStatement findPhenotypes;
    private final Map<String, String> nameToIdMap = new HashMap<>();
    private final Set<String> activityGenes = new HashSet<>();
    private final List<AutoCloseable> closables = new ArrayList<>();
    private final Gson gson = new Gson();

    private DbHarness() throws SQLException {
      this.conn = ConnectionFactory.newConnection();
      this.insert = conn.prepareStatement(
          "insert into test_alert(cds_context, genes, drugid, alert_text, population, activity_score, phenotype) values (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)");
      this.insertNote = conn.prepareStatement(
          "insert into drug_note(drugId, type, ordinal, note) values (?, ?, ?, ?)");
      this.insertChangeStmt = conn.prepareStatement(
          "insert into drug_note(drugid, note, type, ordinal, date) values (?, ?, ?, ?, ?)");
      this.findLookup = conn.prepareStatement("select lookupmethod from gene where symbol=?");
      this.findPhenotypes = conn.prepareStatement("select phenotype from gene_phenotype where genesymbol=?");

      closables.add(this.insert);
      closables.add(this.insertNote);
      closables.add(this.insertChangeStmt);
      closables.add(this.findLookup);
      closables.add(this.findPhenotypes);
      closables.add(conn);
    }

    int noteOrdinal = 0;
    private void writeNote(String note) throws SQLException {
      for (String drugId : nameToIdMap.values()) {
        insertNote.clearParameters();
        insertNote.setString(1, drugId);
        insertNote.setString(2, getNoteType().name());
        insertNote.setInt(3, noteOrdinal);
        insertNote.setString(4, note);
        insertNote.executeUpdate();
      }
      noteOrdinal += 1;
    }

    private void addGenes(Collection<String> genes) throws SQLException {
      for (String gene : genes) {
        if (findLookup(gene) == LookupMethod.ACTIVITY_SCORE) {
          activityGenes.add(gene);
        }
      }
    }

    private void writeHistory(Date date, String note, int ordinal) throws SQLException {
      for (String drugId : nameToIdMap.values()) {
        this.insertChangeStmt.clearParameters();
        this.insertChangeStmt.setString(1, drugId);
        if (StringUtils.isNotBlank(note)) {
          this.insertChangeStmt.setString(2, note);
        } else {
          this.insertChangeStmt.setString(2, "n/a");
        }
        this.insertChangeStmt.setString(3, getNoteType().name());
        this.insertChangeStmt.setInt(4, ordinal);
        this.insertChangeStmt.setDate(5, new java.sql.Date(date.getTime()));
        this.insertChangeStmt.executeUpdate();
      }
    }

    private void writeAlert(String context, Set<String> genes, String drugName, String alertText, String population, Map<String,String> activityMap, Map<String,String> phenotypeMap) throws SQLException {
      Array geneArray = conn.createArrayOf("VARCHAR", genes.toArray());
      Array alertSqlArray = conn.createArrayOf("VARCHAR", new String[]{alertText});

      for (String gene : activityGenes) {
        if (activityMap.get(gene).equals(NA) && !phenotypeMap.get(gene).equals("Indeterminate") && !phenotypeMap.get(gene).startsWith("No Result")) {
          sf_logger.warn("activity gene has no activity score assigned for {}", gson.toJson(phenotypeMap));
        }
      }

      insert.clearParameters();
      insert.setString(1, context);
      insert.setArray(2, geneArray);
      insert.setString(3, lookup(drugName));
      insert.setArray(4, alertSqlArray);
      insert.setString(5, population);
      insert.setString(6, gson.toJson(activityMap));
      insert.setString(7, gson.toJson(phenotypeMap));
      insert.executeUpdate();
    }

    private String lookup(String name) throws SQLException {
      if (StringUtils.isBlank(name)) {
        return null;
      }

      String id = nameToIdMap.get(name);
      if (id == null) {
        id = DbLookup.getDrugByName(conn, name)
                .orElseThrow(() -> new RuntimeException("No drug for " + name));
        nameToIdMap.put(name, id);
      }
      return id;
    }

    @Nullable
    LookupMethod findLookup(String gene) throws SQLException {
      this.findLookup.setString(1, gene);
      try (ResultSet rs = this.findLookup.executeQuery()) {
        if (rs.next()) {
          return LookupMethod.valueOf(rs.getString(1));
        } else {
          sf_logger.warn("no gene data for {}", gene);
          return null;
        }
      }
    }

    @Override
    public void close() throws Exception {
      for (AutoCloseable closable : closables) {
        closable.close();
      }
    }
  }
}
