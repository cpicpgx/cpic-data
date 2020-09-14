package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

import java.sql.*;

public class PairWorkbook extends AbstractWorkbook {

  private static final String SHEET_NAME = "CPIC Gene-Drug Pairs";
  private static final String FILE_NAME = "cpicPairs.xlsx";

  private final SheetWrapper sheet;

  PairWorkbook() {
    super();
    this.sheet = findSheet(SHEET_NAME);
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
    return FILE_NAME;
  }
}
