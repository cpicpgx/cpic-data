package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook of all alleles in the CPIC system
 *
 * @author Ryan Whaley
 */
public class AlleleSummaryWorkbook extends AbstractWorkbook {
  private static final String SHEET_NAME = "Alleles";
  private static final String FILE_NAME = "cpic_alleles.xlsx";
  private final SheetWrapper sheet;

  AlleleSummaryWorkbook() {
    super();
    this.sheet = findSheet(SHEET_NAME);
    this.sheet.setColCount(4);

    Row row = this.sheet.nextRow();
    writeHeaderCell(row, 0, "Gene");
    writeHeaderCell(row, 1, "Allele");
    writeHeaderCell(row, 2, "Guideline");
    writeHeaderCell(row, 3, "URL");
  }

  void writeRow(String gene, String allele, String guideline, String url) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, gene, false);
    writeStringCell(row, 1, allele, false);
    writeStringCell(row, 2, guideline, false);
    writeStringCell(row, 3, url, false);
  }

  @Override
  String getFilename() {
    return FILE_NAME;
  }
}
