package org.cpicpgx.importer;

import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses gene CDS language files
 *
 * @author Ryan Whaley
 */
public class GeneCdsImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern GENE_PATTERN = Pattern.compile("([\\w-]+)\\s+[Pp]henotype");
  private static final String[] sf_deleteStatements = new String[]{
      "delete from file_note where type='" + FileType.GENE_CDS.name() + "'",
      "delete from change_log where type='" + FileType.GENE_CDS.name() + "'"
  };
  private static final int COL_PHENOTYPE = 0;
  private static final int COL_ACTIVITY = 1;
  private static final int COL_EHR_PRIORITY = 2;
  private static final int COL_CONSULTATION = 3;

  public static void main(String[] args) {
    rebuild(new GeneCdsImporter(), args);
  }
  
  public GeneCdsImporter() { }
  
  @Override
  public FileType getFileType() {
    return FileType.GENE_CDS;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }
  
  @Override
  String getFileExtensionToProcess() {
    return Constants.EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    workbook.switchToSheet(0);
    int rowIdx = 0;

    sf_logger.debug("Reading CDS sheet: {}", workbook.currentSheet.getSheetName());

    RowWrapper headerRow = workbook.getRow(rowIdx);
    rowIdx += 1;
    String geneText = headerRow.getNullableText(0);
    if (geneText == null) {
      throw new NotFoundException("Couldn't find gene");
    }
    Matcher m = GENE_PATTERN.matcher(geneText);
    if (!m.find()) {
      sf_logger.warn("No gene found for workbook {}, skipping", workbook.getFileName());
      return;
    }
    String geneSymbol = m.group(1);
    sf_logger.debug("loading gene {}", geneSymbol);

    try (GeneDbHarness dbHarness = new GeneDbHarness(geneSymbol)) {
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        RowWrapper dataRow = workbook.getRow(rowIdx);
        if (dataRow.hasNoText(COL_PHENOTYPE)) continue;

        String activity = normalizeScore(dataRow.getNullableText(COL_ACTIVITY));
        
        String pheno = dataRow.getNullableText(COL_PHENOTYPE);
        if (pheno.toLowerCase().startsWith("notes")) {
          rowIdx++;
          break;
        }

        String priority = dataRow.getText(COL_EHR_PRIORITY);
        String consultation = dataRow.getText(COL_CONSULTATION);

        dbHarness.insert(pheno, activity, priority, consultation);
      }

      List<String> notes = new ArrayList<>();
      // pick up any note rows after "Notes:" header
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        RowWrapper row = workbook.getRow(rowIdx);
        if (row.hasNoText(0)) continue;
        notes.add(row.getText(0));
      }
      writeNotes(geneSymbol, notes);

      try {
        workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
        processChangeLog(dbHarness, workbook, geneSymbol);
      } catch (InvalidParameterException ex) {
        sf_logger.debug("No change log sheet, skipping");
      }
    }
  }

  static class GeneDbHarness extends DbHarness {
    private final String gene;
    private final PreparedStatement insertStmt;
    private final PreparedStatement updateStmt;
    private final boolean phenotypesExist;

    GeneDbHarness(String gene) throws SQLException {
      super(FileType.GENE_CDS);
      this.gene = gene;

      //language=PostgreSQL
      insertStmt = prepare(
          "insert into gene_result(geneSymbol, result, ehrPriority, consultationText, activityScore) " +
              "values (?, ?, ?, ?, ?)"
      );

      //language=PostgreSQL
      updateStmt = prepare("update gene_result set ehrPriority=?, consultationText=? where geneSymbol=? and result=? and activityScore=?");

      //language=PostgreSQL
      PreparedStatement existingStmt = prepare("select count(*) from gene_result where genesymbol=?");
      existingStmt.setString(1, gene);
      try (ResultSet rs = existingStmt.executeQuery()) {
        if (rs.next()) {
          int count = rs.getInt(1);
          phenotypesExist = count > 0;
        } else {
          phenotypesExist = false;
        }
      }
    }

    void insert(String phenotype, String activity, String ehr, String consultation) throws Exception {
      String normalizedPhenotype = phenotype.replaceAll(this.gene + "\\s+", "");

      if (!phenotypesExist) {
        insertStmt.clearParameters();
        insertStmt.setString(1, gene);
        insertStmt.setString(2, normalizedPhenotype);
        insertStmt.setString(3, ehr);
        insertStmt.setString(4, consultation);
        insertStmt.setString(5, activity);
        insertStmt.executeUpdate();
      } else {
        updateStmt.clearParameters();
        setNullableString(updateStmt, 1, ehr);
        setNullableString(updateStmt, 2, consultation);
        updateStmt.setString(3, gene);
        updateStmt.setString(4, normalizedPhenotype);
        updateStmt.setString(5, activity);

        int result = updateStmt.executeUpdate();
        if (result == 0) {
          throw new NotFoundException(String.format("No phenotype row exists for %s %s [activity:%s]", gene, normalizedPhenotype, activity));
        }
      }
    }
  }
}
