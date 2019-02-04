package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * A workbook containing test alert language for gene phenotypes
 *
 * @author Ryan Whaley
 */
class TestAlertWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_TEMPLATE = "%s_Pre_and_Post_Test_Alerts.xlsx";
  
  private String drug;
  private Sheet sheet;
  
  TestAlertWorkbook(String drug, int numTriggers) {
    super();
    this.drug = drug;
    
    this.sheet = getSheet(this.drug);
    this.colIdx = 0;
    
    Row headerRow = this.sheet.createRow(rowIdx++);
    if (numTriggers == 1) {
      writeHeaderCell(headerRow, colIdx++, "Trigger Condition");
    } else {
      for (int i = 1; i <= numTriggers; i++) {
        writeHeaderCell(headerRow, colIdx++, "Trigger Condition " + i);
      }
    }
    writeHeaderCell(headerRow, colIdx++, "Flow Chart Reference Point");
    writeHeaderCell(headerRow, colIdx++, "CDS Context, Relative to Genetic Testing");
    writeHeaderCell(headerRow, colIdx++, "CDS Alert Text");
    
    Integer[] columnSizes = new Integer[colIdx+1];
    columnSizes[colIdx] = 15*256;
    setColumnSizes(columnSizes);
  }
  
  void writeAlert(String[] triggers, String flowRef, String context, String[] alertText) {
    this.colIdx = 0;
    Row row = this.sheet.createRow(rowIdx++);
    for (String trigger : triggers) {
      writeStringCell(row, colIdx++, trigger, false);
    }
    writeStringCell(row, colIdx++, flowRef);
    writeStringCell(row, colIdx++, context);
    writeStringCell(row, colIdx++, String.join("\n\n", alertText), wrapStyle);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug);
  }

  @Override
  String getSheetName() {
    return this.drug;
  }
}
