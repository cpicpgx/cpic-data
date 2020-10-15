package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Importer for gene-drug pairs that pairs with the export of {@link org.cpicpgx.exporter.PairsExporter}
 */
public class PairImporter extends BaseDirectoryImporter {
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

        db.write(
            row.getText(0),
            row.getText(1),
            row.getNullableText(2),
            row.getNullableText(3),
            row.getNullableText(4, true),
            row.getNullableText(5),
            row.getNullablePmids(6),
            row.getNullableText(7),
            row.getText(8),
            row.getNullableDate(9),
            row.getNullableText(10)
        );
      }

      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      db.updateGuidelineGenes();
      processChangeLog(db, workbook, null);

      if (db.unknownDrugs.size() > 0) {
        System.err.println("Missing drugs, create resource files, import them, try again: " + String.join("; ", db.unknownDrugs));
      }
      if (db.unknownGenes.size() > 0) {
        System.err.println("Missing genes, create resource files, import them, try again: " + String.join("; ", db.unknownGenes));
      }
      if (db.existingPairs.size() > 0) {
        System.err.println("Pairs in the DB but missing from input sheet:\n" + String.join("\n", db.existingPairs));
      }
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

  private static class PairDbHarness extends DbHarness {

    final PreparedStatement upsertPair;
    final PreparedStatement updateDrug;
    final PreparedStatement updateGuidelines;
    final Set<String> unknownDrugs = new TreeSet<>();
    final Set<String> unknownGenes = new TreeSet<>();
    final Set<String> existingPairs = new TreeSet<>();

    PairDbHarness() throws SQLException {
      super(FileType.PAIR);
      //language=PostgreSQL
      upsertPair = prepare("insert into pair(genesymbol, drugid, guidelineid, cpiclevel, pgkbcalevel, pgxtesting, citations, usedforrecommendation, removed, removeddate, removedreason) " +
          "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict (genesymbol, drugid) do " +
          "update set guidelineid=excluded.guidelineid, cpiclevel=excluded.cpiclevel, pgkbcalevel=excluded.pgkbcalevel, pgxtesting=excluded.pgxtesting, citations=excluded.citations, usedforrecommendation=excluded.usedforrecommendation, removed=excluded.removed, removeddate=excluded.removeddate, removedreason=excluded.removedreason");

      //language=PostgreSQL
      updateDrug = prepare("update drug set guidelineid=? where drugid=?");

      //language=PostgreSQL
      updateGuidelines = prepare("update guideline set genes=(select array_agg(distinct genesymbol) from pair where guidelineid=id and cpiclevel ~ 'A') where genes is null");

      //language=PostgreSQL
      try (ResultSet rs = prepare("select lower(d.name)||' + '||p.genesymbol from pair p join drug d on p.drugid = d.drugid").executeQuery()) {
        while (rs.next()) {
          existingPairs.add(rs.getString(1));
        }
      }
    }

    void write(String gene, String drugName, String guidelineUrl, String level, String pgkbLevel, String pgxTesting, String[] citations, String used, String removed, Date removedDate, String removedReason) throws SQLException {
      String drugId = findDrug(drugName);
      boolean knownGene = lookupCachedGene(gene);
      if (!knownGene) {
        unknownGenes.add(gene);
      }

      if (drugId == null || !knownGene) return;

      Integer guidelineId = lookupCachedGuideline(guidelineUrl);

      upsertPair.clearParameters();
      setNullableString(upsertPair, 1, StringUtils.stripToNull(gene));
      setNullableString(upsertPair, 2, drugId);
      setNullableInteger(upsertPair, 3, guidelineId);
      setNullableString(upsertPair, 4, StringUtils.stripToNull(level));
      setNullableString(upsertPair, 5, StringUtils.stripToNull(pgkbLevel));
      setNullableString(upsertPair, 6, StringUtils.stripToNull(pgxTesting));
      setNullableArray(upsertPair, 7, citations);
      upsertPair.setBoolean(8, StringUtils.strip(used).equalsIgnoreCase("yes"));
      upsertPair.setBoolean(9, StringUtils.strip(removed).equalsIgnoreCase("yes"));
      setNullableDate(upsertPair, 10, removedDate);
      setNullableString(upsertPair, 11, removedReason);
      upsertPair.executeUpdate();

      if (guidelineId != null) {
        setNullableInteger(updateDrug, 1, guidelineId);
        updateDrug.setString(2, drugId);
        updateDrug.executeUpdate();
      }

      existingPairs.remove(drugName.toLowerCase() + " + " + gene);
    }

    void updateGuidelineGenes() throws SQLException {
      updateGuidelines.executeUpdate();
    }

    String findDrug(String drugName) {
      try {
        return lookupCachedDrug(drugName);
      } catch (Exception ex) {
        if (ex instanceof NotFoundException) {
          unknownDrugs.add(drugName.toLowerCase());
        }
        return null;
      }
    }
  }
}
