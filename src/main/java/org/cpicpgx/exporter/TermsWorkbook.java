package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Workbook that contains the standard CPIC terms
 *
 * @author Ryan Whaley
 */
class TermsWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME = "standardized.terms.xlsx"; 
  private static final String SHEET_NAME = "Terms";
  
  private Sheet sheet;

  TermsWorkbook() {
    super();
    this.sheet = getSheet(getSheetName());
    setColCount(4);
    
    Row headerRow = this.sheet.createRow(rowIdx++);
    writeHeaderCell(headerRow, 0, "Category");
    writeHeaderCell(headerRow, 1, "Term");
    writeHeaderCell(headerRow, 2, "Phenotypic Description");
    writeHeaderCell(headerRow, 3, "Genotypic Description");
  }
  
  void writeTerm(String category, String fnDef, String gxDef, String term) {
    Row row = this.sheet.createRow(rowIdx++);
    writeStringCell(row, 0, category);
    writeStringCell(row, 1, term);
    writeStringCell(row, 2, fnDef, false);
    writeStringCell(row, 3, gxDef, false);
  }
  
  @Override
  String getFilename() {
    return FILE_NAME;
  }

  @Override
  String getSheetName() {
    return SHEET_NAME;
  }
}
