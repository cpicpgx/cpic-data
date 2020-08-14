package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Importer for gene-drug pairs that pairs with the export of {@link org.cpicpgx.exporter.PairsExporter}
 */
public class PairImporter extends BaseDirectoryImporter {
  private static final String DEFAULT_DIRECTORY = "gene_drug_pairs";
  private static final String FILE_SUFFIX = "cpicPairs.xlsx";
  //language=PostgreSQL
  private static final String[] DELETE_STATEMENTS = new String[]{"delete from change_log where type='" + FileType.PAIR.name() + "'"};

  public static void main(String[] args) {
    rebuild(new PairImporter(), args);
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    workbook.switchToSheet(0);

    try (PairDbHarness db = new PairDbHarness()) {
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);

        String[] citations = null;
        if (row.getNullableText(5) != null) {
          citations = row.getText(5).split(";");
        }

        db.write(
            row.getText(0),
            row.getText(1),
            row.getNullableText(6),
            row.getText(2),
            row.getNullableText(3),
            row.getNullableText(4),
            citations,
            row.getNullableText(7)
        );
      }

      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      processChangeLog(db, workbook, null);
    }
  }

  @Override
  String getFileExtensionToProcess() {
    return FILE_SUFFIX;
  }

  @Override
  FileType getFileType() {
    return FileType.PAIR;
  }

  @Override
  String[] getDeleteStatements() {
    return DELETE_STATEMENTS;
  }

  @Override
  String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  private static class PairDbHarness extends DbHarness {

    final PreparedStatement upsertPair;

    PairDbHarness() throws SQLException {
      super(FileType.PAIR);
      //language=PostgreSQL
      String upsertSql = "insert into pair(genesymbol, drugid, guidelineid, level, pgkbcalevel, pgxtesting, citations, usedforrecommendation) " +
          "values (?, ?, ?, ?, ?, ?, ?, ?) on conflict (genesymbol, drugid) do " +
          "update set guidelineid=excluded.guidelineid, level=excluded.level, pgkbcalevel=excluded.pgkbcalevel, pgxtesting=excluded.pgxtesting, citations=excluded.citations, usedforrecommendation=excluded.usedforrecommendation";
      upsertPair = prepare(upsertSql);
    }

    void write(String gene, String drugName, String guidelineUrl, String level, String pgkbLevel, String pgxTesting, String[] citations, String used) throws SQLException {
      upsertPair.clearParameters();
      setNullableString(upsertPair, 1, StringUtils.stripToNull(gene));
      setNullableString(upsertPair, 2, lookupCachedDrug(drugName));
      setNullableInteger(upsertPair, 3, lookupCachedGuideline(guidelineUrl));
      setNullableString(upsertPair, 4, StringUtils.stripToNull(level));
      setNullableString(upsertPair, 5, StringUtils.stripToNull(pgkbLevel));
      setNullableString(upsertPair, 6, StringUtils.stripToNull(pgxTesting));
      setNullableArray(upsertPair, 7, citations);
      upsertPair.setBoolean(8, StringUtils.strip(used).equalsIgnoreCase("yes"));
      upsertPair.executeUpdate();
    }
  }
}
