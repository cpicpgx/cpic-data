package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * A workbook to list diplotype to phenotype mappings for a particular gene
 *
 * @author Ryan Whaley
 */
class DiplotypeWorkbook extends AbstractWorkbook {
  private static final String NAME_TEMPLATE = "%s_Diplotype_Phenotype_Table.xlsx";
  private static final String SHEET_NAME = "Diplotypes";
  private String gene;
  private Sheet sheet;
  
  DiplotypeWorkbook(String gene) {
    super();
    this.gene = gene;
    
    this.sheet = getSheet(getSheetName());
    Row row = sheet.createRow(rowIdx++);
    
    writeHeaderCell(row, 0, String.format("%s Diplotype", this.gene));
    writeHeaderCell(row, 1, "Coded Diplotype/Phenotype Summary");
    writeHeaderCell(row, 2, "EHR Priority Notation");
    writeHeaderCell(row, 3, "Activity Score");
    
    this.colIdx = 3;
  }
  
  void writeDiplotype(String diplotype, String phenotype, String ehr, String activity) {
    Row row = sheet.createRow(rowIdx++);
    writeStringCell(row, 0, diplotype);
    writeStringCell(row, 1, phenotype);
    writeStringCell(row, 2, ehr);
    writeStringCell(row, 3, activity);
  }
  
  @Override
  String getFilename() {
    return String.format(NAME_TEMPLATE, this.gene);
  }

  @Override
  String getSheetName() {
    return SHEET_NAME;
  }
}
