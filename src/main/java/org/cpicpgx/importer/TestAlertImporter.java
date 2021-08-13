package org.cpicpgx.importer;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
      "delete from change_log where type='" + FileType.TEST_ALERT.name() + "'",
      "delete from file_note where type='" + FileType.TEST_ALERT.name() + "'",
      "delete from test_alert"
  };
  private static final String FILE_EXTENSION = "_Pre_and_Post_Test_Alerts.xlsx";

  private final List<String> f_notes = new ArrayList<>();

  public static void main(String[] args) {
    rebuild(new TestAlertImporter(), args);
  }
  
  public TestAlertImporter() {}

  @Override
  public FileType getFileType() {
    return FileType.TEST_ALERT;
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
    try (TestDbHarness dbHarness = new TestDbHarness()) {
      for (Iterator<Sheet> sheetIterator = workbook.getSheetIterator(); sheetIterator.hasNext(); ) {
        Sheet sheet = sheetIterator.next();
        workbook.currentSheetIs(sheet.getSheetName());

        if (AbstractWorkbook.HISTORY_SHEET_NAME.equalsIgnoreCase(sheet.getSheetName())) {
          processHistory(workbook, dbHarness);
        }
        else if (!sheet.getSheetName().equalsIgnoreCase("flow chart")) {
          String population = sheet.getSheetName().replaceFirst("population ", "");
          processTestAlertSheet(workbook, dbHarness, population);
        }
      }
      for (String drugId : dbHarness.getDrugIds()) {
        writeNotes(drugId, f_notes);
      }
      f_notes.clear();
    }
  }

  private static final int COL_DRUG = 0;
  private static final int COL_TRIGGER_START = 1;
  private static final String COL_NAME_CONTEXT = "CDS Context, Relative to Genetic Testing";
  private static final String COL_NAME_ALERT = "CDS Alert Text";
  private static final Pattern PHENO_PATTERN = Pattern.compile("([\\w-]+)\\s+[Pp]henotype");
  private static final Pattern AS_PATTERN = Pattern.compile("([\\w-]+)?\\s*[Aa]ctivity [Ss]core.*");
  private static final Pattern ALLELE_PATTERN = Pattern.compile("([\\w-]+)\\s+[Aa]llele.*");

  private void processTestAlertSheet(WorkbookWrapper workbook, TestDbHarness dbHarness, String population) throws Exception {
    RowWrapper headerRow = workbook.getRow(0);

    int idxContext = -1;
    int idxAlert = -1;
    Map<String, Integer> idxPhenotypeByGene = new HashMap<>();
    Map<String, Integer> idxActivityByGene = new HashMap<>();
    Map<String, Integer> idxAlleleByGene = new HashMap<>();

    for (int h=COL_TRIGGER_START; h<= headerRow.row.getLastCellNum(); h++) {
      String columnHeaderText = StringUtils.strip(headerRow.getNullableText(h));
      if (columnHeaderText == null) {
        continue;
      }

      Matcher phenoMatch = PHENO_PATTERN.matcher(columnHeaderText);
      Matcher activityMatch = AS_PATTERN.matcher(columnHeaderText);
      Matcher alleleMatch = ALLELE_PATTERN.matcher(columnHeaderText);

      if (columnHeaderText.equals(COL_NAME_CONTEXT)) {
        idxContext = h;
      } else if (columnHeaderText.equals(COL_NAME_ALERT)) {
        idxAlert = h;
      } else if (phenoMatch.matches() && phenoMatch.group(1) != null) {
        idxPhenotypeByGene.put(phenoMatch.group(1), h);
      } else if (activityMatch.matches() && activityMatch.group(1) != null) {
        idxActivityByGene.put(activityMatch.group(1), h);
      } else if (alleleMatch.matches() && alleleMatch.group(1) != null) {
        idxAlleleByGene.put(alleleMatch.group(1), h);
      } else {
        throw new RuntimeException("Column type not recognized: " + columnHeaderText);
      }
    }

    if (idxPhenotypeByGene.size() == 0 && idxActivityByGene.size() == 0 && idxAlleleByGene.size() == 0) {
      throw new RuntimeException("No lookup columns found");
    }
    if (idxContext == -1) {
      sf_logger.warn("no context column found");
    }
    if (idxAlert == -1) {
      sf_logger.warn("no alert text column found");
    }

    dbHarness.addGenes(idxPhenotypeByGene.keySet());
    dbHarness.addGenes(idxActivityByGene.keySet());
    dbHarness.addGenes(idxAlleleByGene.keySet());

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
      for (String gene : idxActivityByGene.keySet()) {
        String pheno = normalizeGeneText(gene, row.getText(idxPhenotypeByGene.get(gene)));
        if (pheno != null && Constants.isNoResult(pheno)) {
          activityJson.put(gene, pheno);
        } else {
          activityJson.put(gene, normalizeScore(normalizeGeneText(gene, row.getText(idxActivityByGene.get(gene)))));
        }
      }
      Map<String,String> phenotypeJson = new HashMap<>();
      for (String gene : idxPhenotypeByGene.keySet()) {
        String normalizedPhenotype = normalizeGeneText(gene, row.getText(idxPhenotypeByGene.get(gene)));
        phenotypeJson.put(gene, dbHarness.validPhenotype(gene, normalizedPhenotype));
      }

      Map<String,String> alleleJson = new HashMap<>();
      for (String gene : idxAlleleByGene.keySet()) {
        alleleJson.put(gene, normalizeGeneText(gene, row.getText(idxAlleleByGene.get(gene))));
      }

      String context = StringUtils.replace(row.getText(idxContext), "Test", "test");

      try {
        dbHarness.writeAlert(context, row.getText(COL_DRUG), row.getText(idxAlert), population, activityJson, phenotypeJson, alleleJson);
      } catch (Exception ex) {
        throw new RuntimeException("Error processing row " + (i + 1), ex);
      }
    }
  }

  void processHistory(WorkbookWrapper workbook, TestDbHarness dbHarness) throws SQLException {
    for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
      RowWrapper row = workbook.getRow(i);
      if (row.hasNoText(0) ^ row.hasNoText(1)) {
        throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
      } else if (row.hasNoText(0)) continue;

      java.util.Date date = row.getDate(0);
      String note = row.getText(1);
      dbHarness.writeHistory(date, note);
    }
  }

  private static class TestDbHarness extends DbHarness {
    private final PreparedStatement insert;
    private final PreparedStatement findLookup;
    private final Map<String, String> nameToIdMap = new HashMap<>();
    private final Map<String,LookupMethod> geneMap = new HashMap<>();
    private final Gson gson = new Gson();

    private TestDbHarness() throws SQLException {
      super(FileType.TEST_ALERT);
      this.insert = prepare(
          "insert into test_alert(cdsContext, genes, drugid, alertText, population, activityScore, phenotype, alleleStatus, lookupKey) values (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb)");
      this.findLookup = prepare("select lookupmethod from gene where symbol=?");
    }

    private Set<String> getGenes() {
      return this.geneMap.keySet();
    }

    private void addGenes(Collection<String> genes) throws SQLException {
      for (String gene : genes) {
        LookupMethod lookup = findLookup(gene);
        geneMap.put(gene, lookup);
      }
    }

    private void writeHistory(Date date, String note) throws SQLException {
      for (String drugId : nameToIdMap.values()) {
        writeChangeLog(drugId, date, note);
      }
    }

    private void writeAlert(String context, String drugName, String alertText, String population, Map<String,String> activityMap, Map<String,String> phenotypeMap, Map<String,String> alleleMap) throws SQLException, NotFoundException {
      Array geneArray = createArrayOf(getGenes().toArray(new String[]{}));
      Array alertSqlArray = createArrayOf(new String[]{alertText});

      Map<String,String> lookupKey = new HashMap<>();
      for (String gene : getGenes()) {
        if (this.geneMap.get(gene) == LookupMethod.PHENOTYPE) {
          if (phenotypeMap.get(gene) != null) {
            lookupKey.put(gene, phenotypeMap.get(gene));
            alleleMap.remove(gene);
          } else {
            throw new RuntimeException("No phenotype value for gene " + gene);
          }
        }
        else if (this.geneMap.get(gene) == LookupMethod.ACTIVITY_SCORE) {
          if (activityMap.get(gene) != null) {
            lookupKey.put(gene, activityMap.get(gene));
            alleleMap.remove(gene);
          } else {
            throw new RuntimeException("No activity score value for gene " + gene);
          }
        }
        else if (this.geneMap.get(gene) == LookupMethod.ALLELE_STATUS) {
          if (alleleMap.get(gene) != null) {
            lookupKey.put(gene, alleleMap.get(gene));
          } else {
            throw new RuntimeException("No allele status value for gene " + gene);
          }
        }
      }

      insert.clearParameters();
      insert.setString(1, context);
      insert.setArray(2, geneArray);
      insert.setString(3, lookupCachedDrug(drugName));
      insert.setArray(4, alertSqlArray);
      insert.setString(5, population);
      insert.setString(6, gson.toJson(activityMap));
      insert.setString(7, gson.toJson(phenotypeMap));
      insert.setString(8, gson.toJson(alleleMap));
      insert.setString(9, gson.toJson(lookupKey));
      insert.executeUpdate();
    }

    private Collection<String> getDrugIds() {
      return nameToIdMap.values();
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
  }
}
