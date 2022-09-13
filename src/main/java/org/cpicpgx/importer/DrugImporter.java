package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Creates and updates drug records based on a directory of individual files, one per drug.
 */
public class DrugImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FILE_SUFFIX = "-Drug_Resource_Mappings.xlsx";

  // purposely DO NOT delete anything, this import only upserts records
  private static final String[] sf_deleteStatements = new String[0];

  private static final int ROW_RXNORM = 1;
  private static final int ROW_DRUGBANK = 2;
  private static final int ROW_ATC = 3;
  private static final int ROW_PGKB = 4;
  private static final int COL_NAME = 0;
  private static final int COL_ID = 3;

  public static void main(String[] args) {
    rebuild(new DrugImporter(), args);
  }

  @Override
  String getFileExtensionToProcess() {
    return FILE_SUFFIX;
  }

  @Override
  FileType getFileType() {
    return FileType.DRUG_RESOURCE;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    workbook.switchToSheet(0);

    RowWrapper rxnormRow = workbook.getRow(ROW_RXNORM);
    String rxnormDrugName = rxnormRow.getText(COL_NAME);
    String rxNormId = rxnormRow.getNullableText(COL_ID, true);

    RowWrapper drugbankRow = workbook.getRow(ROW_DRUGBANK);
    String drugbankId = drugbankRow.getNullableText(COL_ID);

    RowWrapper atcRow = workbook.getRow(ROW_ATC);
    String atcId = atcRow.getNullableText(COL_ID);
    List<String> atcArray = new ArrayList<>();
    if (StringUtils.isNotBlank(atcId)) {
      Arrays.stream(atcId.split("[;,]"))
          .map(StringUtils::strip)
          .forEach(atcArray::add);
    }

    RowWrapper pgkbRow = workbook.getRow(ROW_PGKB);
    String pgkbId = pgkbRow.getNullableText(COL_ID);
    String pgkbName = pgkbRow.getText(COL_NAME);

    if (!Objects.equals(pgkbName, rxnormDrugName)) {
      sf_logger.info("Drug name differs from RxNorm\nPGKB   = {}\nRxNorm = {}", pgkbName, rxnormDrugName);
    }

    try (Connection conn = ConnectionFactory.newConnection()) {
      String drugId;
      boolean newDrug = true;

      try (PreparedStatement findName = conn.prepareStatement("select drugid from drug where lower(name)=?")) {
        findName.setString(1, pgkbName.toLowerCase());
        ResultSet rs = findName.executeQuery();
        if (rs.next()) {
          drugId = rs.getString(1);
          newDrug = false;
        } else {
          if (rxNormId != null) {
            drugId = "RxNorm:" + rxNormId;
          } else if (atcArray.size() > 0) {
            drugId = "ATC:" + atcArray.get(0);
          } else if (drugbankId != null) {
            drugId = "Drugbank:" + drugbankId;
          } else {
            throw new RuntimeException("Could not determine a source for drug ID");
          }
        }
      }

      try (PreparedStatement insert = conn.prepareStatement(
          "insert into drug(drugid, name, pharmgkbid, rxnormid, drugbankid, atcid) " +
              "values (?, ?, ?, ?, ?, ?) on conflict (drugid) " +
              "do update set drugbankid=excluded.drugBankid, pharmgkbid=excluded.pharmgkbid, " +
              "rxnormid=excluded.rxnormid, atcId=excluded.atcid"
      )) {
        insert.setString(1, drugId);
        insert.setString(2, pgkbName);
        if (pgkbId != null) {
          insert.setString(3, pgkbId);
        } else {
          insert.setNull(3, Types.VARCHAR);
        }
        if (rxNormId != null) {
          insert.setString(4, rxNormId);
        } else {
          insert.setNull(4, Types.VARCHAR);
        }
        if (drugbankId != null) {
          insert.setString(5, drugbankId);
        } else {
          insert.setNull(5, Types.VARCHAR);
        }
        if (atcArray.size() > 0) {
          insert.setArray(6, conn.createArrayOf("TEXT", atcArray.toArray(new String[0])));
        } else {
          insert.setNull(6, Types.ARRAY);
        }
        int insCount = insert.executeUpdate();

        if (insCount == 1 && newDrug) {
          sf_logger.debug("inserted new drug: {} {}", drugId, pgkbName);
        } else {
          sf_logger.debug("updated drug: {} {}", drugId, pgkbName);
        }
      }
    }
  }
}
