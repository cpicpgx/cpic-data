package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook for example CDS language for a given Gene
 *
 * @author Ryan Whaley
 */
class GeneCdsWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_PATTERN = "%s-CDS.xlsx";
  private static final String SHEET_NAME = "CDS";
  private String geneSymbol;
  private SheetWrapper sheetWrapper;

  GeneCdsWorkbook(String geneSymbol) {
    super();
    this.geneSymbol = geneSymbol;
    this.sheetWrapper = findSheet(SHEET_NAME);
    
    Row geneRow = sheetWrapper.nextRow();
    writeHeaderCell(geneRow, 0, "Gene: " + geneSymbol);
    Row headerRow = sheetWrapper.nextRow();
    writeHeaderCell(headerRow, 0, "Phenotype");
    writeHeaderCell(headerRow, 1, "EHR Priority Result Notation");
    writeHeaderCell(headerRow, 2, "Consultation (Interpretation) Text Provided with Test Result");
    writeHeaderCell(headerRow, 3, "Notes");
    sheetWrapper.setColCount(4);
    sheetWrapper.setWidths(new Integer[]{50*256, 50*256, 150*256, 150*256});
  }
  
  @Override
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
  
  void writeConsultation(String phenotype, String priority, String consultation, String notes) {
    Row headerRow = sheetWrapper.nextRow();
    writeStringCell(headerRow, 0, phenotype);
    writeStringCell(headerRow, 1, priority);
    writeStringCell(headerRow, 2, consultation, false);
    writeStringCell(headerRow, 3, notes, false);
  }
  
  private int nNote = 0;
  void writeNote(String note) {
    if (nNote == 0) {
      this.sheetWrapper.nextRow();
      Row headerRow = this.sheetWrapper.nextRow();
      writeStringCell(headerRow, 0, "Note", false);
    }
    Row row = this.sheetWrapper.nextRow();
    writeStringCell(row, 0, note, false);
    nNote += 1;
  }
}
