package org.cpicpgx.importer;

import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
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
    return EXCEL_EXTENSION;
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

    GeneDbHarness(String gene) throws SQLException {
      super(FileType.GENE_CDS);
      this.gene = gene;

      insertStmt = prepare(
          "insert into gene_phenotype(geneSymbol, phenotype, ehrPriority, consultationText, activityScore) values (?, ?, ?, ?, ?) ON CONFLICT (genesymbol, phenotype, activityScore) DO UPDATE set ehrpriority=excluded.ehrpriority, consultationtext=excluded.consultationtext"
      );
    }

    void insert(String phenotype, String activity, String ehr, String consultation) throws SQLException {
      String normalizedPhenotype = phenotype.replaceAll(this.gene + "\\s+", "");

      insertStmt.clearParameters();
      insertStmt.setString(1, gene);
      insertStmt.setString(2, normalizedPhenotype);
      insertStmt.setString(3, ehr);
      insertStmt.setString(4, consultation);
      insertStmt.setString(5, activity);
      insertStmt.executeUpdate();
    }
  }
}
