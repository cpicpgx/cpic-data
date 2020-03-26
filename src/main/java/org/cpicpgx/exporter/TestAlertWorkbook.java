package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

import java.util.List;

/**
 * A workbook containing test alert language for gene phenotypes
 *
 * @author Ryan Whaley
 */
class TestAlertWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_TEMPLATE = "%s_Pre_and_Post_Test_Alerts.xlsx";
  
  private String drug;
  private SheetWrapper sheet;
  
  TestAlertWorkbook(String drug, int numTriggers) {
    super();
    this.drug = drug;
    
    this.sheet = findSheet(this.drug);
    this.colIdx = 0;
    
    Row headerRow = this.sheet.nextRow();
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
    
    this.sheet.setColCount(colIdx+1);
    Integer[] columnSizes = new Integer[colIdx+1];
    columnSizes[colIdx] = 15*256;
    this.sheet.setWidths(columnSizes);
  }
  
  void writeAlert(String[] triggers, String context, String[] alertText) {
    this.colIdx = 0;
    Row row = this.sheet.nextRow();
    for (String trigger : triggers) {
      writeStringCell(row, colIdx++, trigger, false);
    }
    writeStringCell(row, colIdx++, context);
    writeStringCell(row, colIdx++, String.join("\n\n", alertText), wrapStyle);
  }
  
  void writeNotes(List<String> notes) {
    if (notes == null || notes.size() == 0) return;
    
    this.sheet.nextRow();
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, "Notes:", false);
    for (String note : notes) {
      row = this.sheet.nextRow();
      writeStringCell(row, 0, note, false);
    }
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug);
  }
}
