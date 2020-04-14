package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

/**
 * A representation of a Recommendation workbook
 *
 * @author Ryan Whaley
 */
class RecommendationWorkbook extends AbstractWorkbook {
  
  private static final String FILE_NAME_TEMPLATE = "%s-Recommendations.xlsx";
  private static final String SHEET_NAME = "Recommendations";
  
  private SheetWrapper sheet;
  private String drug;
  
  RecommendationWorkbook(String drug) {
    super();
    
    this.sheet = findSheet(SHEET_NAME);
    this.drug = drug;

    Row headerRow = this.sheet.nextRow();
    writeHeaderCell(headerRow, 0, "Phenotype");
    writeHeaderCell(headerRow, 1, "Implication");
    writeHeaderCell(headerRow, 2, "Therapeutic Recommendation");
    writeHeaderCell(headerRow, 3, "Classification of Recommendation");
    this.sheet.setColCount(4);
    
    Integer[] columnSizes = new Integer[4];
    columnSizes[1] = 60*256;
    this.sheet.setWidths(columnSizes);
  }
  
  void writeRec(String pheno, String impl, String rec, String classification) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, pheno, false);
    writeStringCell(row, 1, impl, this.wrapStyle);
    writeStringCell(row, 2, rec, false);
    writeStringCell(row, 3, classification, false);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug);
  }
}
