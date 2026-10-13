package org.cpicpgx.workbook;

import org.apache.poi.ss.usermodel.*;
import org.cpicpgx.db.LookupMethod;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that frequency values on the "References" sheet are stored as numbers with the workbook's
 * decimal/exponential formats, rather than as pre-stringified text that Excel cannot format.
 */
class FrequencyWorkbookReferenceFormatTest {

  private static final DataFormatter FORMATTER = new DataFormatter();

  /** Writes one population row of frequencies and hands back the resulting cells. */
  private static Row writeRow(List<String> frequencies) {
    FrequencyWorkbook wb = new FrequencyWorkbook("CYP3A4", LookupMethod.PHENOTYPE);
    wb.writeReferenceHeader(new LinkedHashSet<>(Arrays.asList("*1", "*2", "*3", "*4")));
    wb.writePopulation(
        new String[]{"Author"}, 2020, "12345", "European", "pop", "", "healthy", 100, frequencies);
    Sheet sheet = wb.findSheet(FrequencyWorkbook.SHEET_NAME_REFERENCES).sheet;
    return sheet.getRow(sheet.getLastRowNum());
  }

  /** Column index of the first allele frequency column on the References sheet. */
  private static final int FIRST_FREQ_COL = 8;

  @Test
  void smallFrequencyIsNumericAndDisplaysExponential() {
    Row row = writeRow(Arrays.asList("0.0000147000455", "", "", ""));
    Cell cell = row.getCell(FIRST_FREQ_COL);

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals(0.0000147000455, cell.getNumericCellValue(), 1e-18);
    assertEquals("1.47E-05", FORMATTER.formatCellValue(cell));
  }

  @Test
  void ordinaryFrequencyIsNumericAndDisplaysFourDecimals() {
    Row row = writeRow(Arrays.asList("0.9382002", "", "", ""));
    Cell cell = row.getCell(FIRST_FREQ_COL);

    assertEquals(CellType.NUMERIC, cell.getCellType());
    assertEquals(0.9382002, cell.getNumericCellValue(), 1e-12);
    assertEquals("0.9382", FORMATTER.formatCellValue(cell));
  }

  @Test
  void fullPrecisionIsRetainedEvenThoughDisplayIsRounded() {
    Row row = writeRow(Arrays.asList("0.015350877", "", "", ""));
    Cell cell = row.getCell(FIRST_FREQ_COL);

    assertEquals("0.0154", FORMATTER.formatCellValue(cell));
    // the underlying value must still be exact, not the rounded display value
    assertEquals(0.015350877, cell.getNumericCellValue(), 1e-15);
  }

  @Test
  void nonNumericLabelStaysText() {
    Row row = writeRow(Arrays.asList("not reported", "", "", ""));
    Cell cell = row.getCell(FIRST_FREQ_COL);

    assertEquals(CellType.STRING, cell.getCellType());
    assertEquals("not reported", cell.getStringCellValue());
  }

  @Test
  void blankFrequencyStaysBlankText() {
    Row row = writeRow(Arrays.asList("", "", "", ""));
    Cell cell = row.getCell(FIRST_FREQ_COL);

    assertEquals(CellType.STRING, cell.getCellType());
    assertEquals("", cell.getStringCellValue());
  }
}
