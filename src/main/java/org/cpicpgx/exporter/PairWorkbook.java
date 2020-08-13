package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

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
      writeStringCell(dataRow, colIdx, rs.getString(colIdx+1), false);
    }
  }

  @Override
  String getFilename() {
    return FILE_NAME;
  }
}
