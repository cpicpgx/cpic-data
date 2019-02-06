package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * A representation of a Recommendation workbook
 *
 * @author Ryan Whaley
 */
class RecommendationWorkbook extends AbstractWorkbook {
  
  private static final String FILE_NAME_TEMPLATE = "%s_Recommendations.xlsx";
  private static final String SHEET_NAME = "Recommendations";
  
  private Sheet sheet;
  private String drug;
  
  RecommendationWorkbook(String drug) {
    super();
    
    this.sheet = getSheet(getSheetName());
    this.drug = drug;
    setColCount(4);

    Row headerRow = this.sheet.createRow(rowIdx++);
    writeHeaderCell(headerRow, 0, "Phenotype");
    writeHeaderCell(headerRow, 1, "Implication");
    writeHeaderCell(headerRow, 2, "Therapeutic Recommendation");
    writeHeaderCell(headerRow, 3, "Classification of Recommendation");
    
    Integer[] columnSizes = new Integer[4];
    columnSizes[1] = 60*256;
    setColumnSizes(columnSizes);
  }
  
  void writeRec(String pheno, String impl, String rec, String classification) {
    this.colIdx = 0;
    Row row = this.sheet.createRow(rowIdx++);
    writeStringCell(row, colIdx++, pheno, false);
    writeStringCell(row, colIdx++, impl, this.wrapStyle);
    writeStringCell(row, colIdx++, rec, false);
    writeStringCell(row, colIdx++, classification, false);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug);
  }

  @Override
  String getSheetName() {
    return SHEET_NAME;
  }
}
