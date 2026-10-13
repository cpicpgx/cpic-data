package org.cpicpgx.workbook;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that numeric cells written by {@link AbstractWorkbook} are actually numeric, so that the
 * exponential/decimal/zero number formats Excel applies are not silently ignored.
 */
class AbstractWorkbookNumberFormatTest {

  /** Minimal concrete workbook so the abstract base can be exercised directly. */
  private static class TestWorkbook extends AbstractWorkbook {
    @Override
    public String getFilename() {
      return "test.xlsx";
    }

    Row newRow() {
      return findSheet("test").sheet.createRow(this.rowNum++);
    }

    private int rowNum = 0;

    Cell writeDecimal(BigDecimal value) {
      Row row = newRow();
      write(row, 0, value);
      return row.getCell(0);
    }

    Cell writeHighlight(BigDecimal value) {
      Row row = newRow();
      writeHighlightCell(row, 0, value);
      return row.getCell(0);
    }
  }

  @Test
  void smallValueIsWrittenAsNumberWithExponentialFormat() {
    TestWorkbook wb = new TestWorkbook();
    Cell cell = wb.writeDecimal(new BigDecimal("0.0000024757005"));

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals(0.0000024757005, cell.getNumericCellValue(), 1e-18);
    assertEquals("0.00E+00", cell.getCellStyle().getDataFormatString());
  }

  @Test
  void ordinaryValueIsWrittenAsNumberWithDecimalFormat() {
    TestWorkbook wb = new TestWorkbook();
    Cell cell = wb.writeDecimal(new BigDecimal("0.0186"));

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals(0.0186, cell.getNumericCellValue(), 1e-12);
    assertEquals("0.0000", cell.getCellStyle().getDataFormatString());
  }

  @Test
  void scaledZeroUsesZeroFormatNotExponential() {
    TestWorkbook wb = new TestWorkbook();
    // a numeric(,4) zero from the DB arrives with scale 4, which is not equal() to BigDecimal.ZERO
    Cell cell = wb.writeDecimal(new BigDecimal("0.0000"));

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals("0", cell.getCellStyle().getDataFormatString());
  }

  @Test
  void unscaledZeroUsesZeroFormat() {
    TestWorkbook wb = new TestWorkbook();
    Cell cell = wb.writeDecimal(BigDecimal.ZERO);

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals("0", cell.getCellStyle().getDataFormatString());
  }

  @Test
  void smallHighlightValueDisplaysExponentialNotZero() {
    TestWorkbook wb = new TestWorkbook();
    Cell cell = wb.writeHighlight(new BigDecimal("0.0000116"));

    assertEquals(CellType.NUMERIC, cell.getCellType());
    // a real frequency must not render as "0.0000", which reads as zero
    assertEquals("0.00E+00", cell.getCellStyle().getDataFormatString());
    assertEquals("1.16E-05", new DataFormatter().formatCellValue(cell));
  }

  @Test
  void zeroHighlightValueDisplaysAsZero() {
    TestWorkbook wb = new TestWorkbook();
    Cell cell = wb.writeHighlight(new BigDecimal("0.0000"));

    assertEquals("0", cell.getCellStyle().getDataFormatString());
    assertEquals("0", new DataFormatter().formatCellValue(cell));
  }

  @Test
  void highlightCellKeepsHighlightFill() {
    TestWorkbook wb = new TestWorkbook();
    // the yellow fill must survive on every magnitude branch, not just the decimal one
    for (String v : new String[]{"0.0000116", "0.0186", "0.0000"}) {
      Cell cell = wb.writeHighlight(new BigDecimal(v));
      assertEquals(FillPatternType.SOLID_FOREGROUND, cell.getCellStyle().getFillPattern(), v);
      assertEquals(IndexedColors.LIGHT_YELLOW.index, cell.getCellStyle().getFillForegroundColor(), v);
    }
  }

  @Test
  void highlightCellIsWrittenAsNumber() {
    TestWorkbook wb = new TestWorkbook();
    Cell cell = wb.writeHighlight(new BigDecimal("0.0186"));

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals(0.0186, cell.getNumericCellValue(), 1e-12);
  }
}
