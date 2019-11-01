package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook to list IDs of a gene in external resources (e.g. NCBI, HGNC)
 *
 * @author Ryan Whaley
 */
class GeneResourceWorkbook extends AbstractWorkbook {
  private static final String NAME_TEMPLATE = "%s-Gene_Resource_Mappings.xlsx";
  private static final String SHEET_NAME = "mapping";
  private String gene;
  private SheetWrapper sheet;
  
  GeneResourceWorkbook(String gene) {
    super();
    this.gene = gene;
    
    this.sheet = findSheet(SHEET_NAME);
    
    Row row = this.sheet.nextRow();
    writeHeaderCell(row, 0, "Gene Symbol");
    writeHeaderCell(row, 1, "Source");
    writeHeaderCell(row, 2, "Code Type");
    writeHeaderCell(row, 3, "Code");
    this.sheet.setColCount(4);
    
    this.colIdx = 3;
  }
  
  void writeMapping(String source, String type, String code) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, this.gene, false);
    writeStringCell(row, 1, source, false);
    writeStringCell(row, 2, type, false);
    writeStringCell(row, 3, code, false);
  }
  
  String getFilename() {
    return String.format(NAME_TEMPLATE, this.gene);
  }
  
  String getSheetName() {
    return SHEET_NAME;
  }
}
