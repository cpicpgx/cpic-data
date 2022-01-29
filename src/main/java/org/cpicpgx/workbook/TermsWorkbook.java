package org.cpicpgx.workbook;

import org.apache.poi.ss.usermodel.Row;

/**
 * Workbook that contains the standard CPIC terms
 *
 * @author Ryan Whaley
 */
public class TermsWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME = "standardized.terms.xlsx"; 
  private static final String SHEET_NAME = "Terms";
  
  private SheetWrapper sheet;

  public TermsWorkbook() {
    super();
    this.sheet = findSheet(SHEET_NAME);
    sheet.setColCount(4);
    
    Row headerRow = sheet.nextRow();
    writeHeaderCell(headerRow, 0, "Category");
    writeHeaderCell(headerRow, 1, "Term");
    writeHeaderCell(headerRow, 2, "Phenotypic Description");
    writeHeaderCell(headerRow, 3, "Genotypic Description");
  }

  public void writeTerm(String category, String fnDef, String gxDef, String term) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, category);
    writeStringCell(row, 1, term);
    writeStringCell(row, 2, fnDef, false);
    writeStringCell(row, 3, gxDef, false);
  }
  
  @Override
  public String getFilename() {
    return FILE_NAME;
  }
}
