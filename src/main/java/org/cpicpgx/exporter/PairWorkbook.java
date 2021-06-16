package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

import java.sql.*;

public class PairWorkbook extends AbstractWorkbook {

  private static final String SHEET_NAME = "CPIC Gene-Drug Pairs";
  private static final String ALL_FILE_NAME = "cpicPairs.xlsx";
  private static final String CURRENT_FILE_NAME = "cpic_gene-drug_pairs.xlsx";

  private final SheetWrapper sheet;
  private final boolean includeRemoved;

  PairWorkbook(boolean includeRemoved) {
    super();
    this.sheet = findSheet(SHEET_NAME);
    this.includeRemoved = includeRemoved;
  }

  void writeHeader(ResultSetMetaData metadata) throws SQLException {
    this.sheet.setColCount(metadata.getColumnCount());
    Row headerRow = sheet.nextRow();
    for (int colIdx = 0; colIdx < metadata.getColumnCount(); colIdx++) {
      writeHeaderCell(headerRow, colIdx, metadata.getColumnName(colIdx+1));
    }
  }

  void writePair(ResultSet rs) throws SQLException {
    Row dataRow = sheet.nextRow();
    for (int colIdx = 0; colIdx < rs.getMetaData().getColumnCount(); colIdx++) {
      Object colObject = rs.getObject(colIdx + 1);
      if (colObject == null) {
        continue;
      } else if (colObject instanceof String) {
        writeStringCell(dataRow, colIdx, rs.getString(colIdx + 1), false);
      } else if (colObject instanceof java.util.Date) {
        writeDateCell(dataRow, colIdx, rs.getDate(colIdx + 1));
      } else {
        throw new RuntimeException("Cell type not supported in column " + (colIdx + 1) + " " + colObject.getClass().getSimpleName());
      }
    }
  }

  @Override
  String getFilename() {
    if (includeRemoved) {
      return ALL_FILE_NAME;
    } else {
      return CURRENT_FILE_NAME;
    }
  }
}
