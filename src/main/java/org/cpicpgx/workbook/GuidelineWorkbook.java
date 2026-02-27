package org.cpicpgx.workbook;

import org.apache.poi.ss.usermodel.Row;

import java.sql.Array;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Workbook to list all basic CPIC guideline information about names, URLs, and other associated entities
 */
public class GuidelineWorkbook extends AbstractWorkbook {
  public static final int IDX_NAME = 0;
  public static final int IDX_CLINPGXID = 1;
  public static final int IDX_GENES = 2;
  public static final int IDX_DRUGS = 3;
  public static final int IDX_PMIDS = 4;
  public static final int IDX_NOTES = 5;
  public static final String FILE_NAME = "cpic_guidelines.xlsx";
  private static final String SHEET_NAME = "Guidelines";

  private final SheetWrapper sheet;

  public GuidelineWorkbook() {
    super();
    this.sheet = findSheet(SHEET_NAME);
  }

  @Override
  public String getFilename() {
    return FILE_NAME;
  }

  public void writeHeader(ResultSetMetaData metadata) throws SQLException {
    writeHeader(this.sheet, metadata);
  }

  public void write(String name, String clinpgxid, Array genes, String notes, Array pmids, Array drugs) throws SQLException {
    Row row = sheet.nextRow();
    writeStringCell(row, IDX_NAME, name, false);
    writeStringCell(row, IDX_CLINPGXID, clinpgxid, false);
    writeStringCell(row, IDX_GENES, concatArray(genes), false);
    writeStringCell(row, IDX_DRUGS, concatArray(drugs), false);
    writeStringCell(row, IDX_PMIDS, concatArray(pmids), false);
    writeStringCell(row, IDX_NOTES, notes, false);
  }

  private static String concatArray(Array arrayValues) throws SQLException {
    if (arrayValues != null) {
      String[] values = (String[])arrayValues.getArray();
      if (values.length == 0 || Arrays.stream(values).allMatch(Objects::isNull)) {
        return "";
      }
      return String.join("; ", (String[])arrayValues.getArray());
    } else {
      return "";
    }
  }
}
