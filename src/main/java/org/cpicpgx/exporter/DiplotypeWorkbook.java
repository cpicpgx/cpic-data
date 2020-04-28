package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook to list diplotype to phenotype mappings for a particular gene
 *
 * @author Ryan Whaley
 */
class DiplotypeWorkbook extends AbstractWorkbook {
  private static final String NAME_TEMPLATE = "%s-Diplotype_Phenotype_Table.xlsx";
  private static final String SHEET_NAME = "Diplotypes";
  private static final String NOTES_SHEET_NAME = "Interpretation Consult Note";
  private final String gene;
  private final SheetWrapper dataSheet;
  private SheetWrapper noteSheet = null;
  
  DiplotypeWorkbook(String gene) {
    super();
    this.gene = gene;
    
    this.dataSheet = findSheet(SHEET_NAME);
    this.dataSheet.setColCount(4);
    Row row = this.dataSheet.nextRow();
    
    writeHeaderCell(row, 0, String.format("%s Diplotype", this.gene));
    writeHeaderCell(row, 1, "Coded Diplotype/Phenotype Summary");
    writeHeaderCell(row, 2, "Activity Score");
    writeHeaderCell(row, 3, "EHR Priority Notation");
  }
  
  void writeDiplotype(String diplotype, String phenotype, String ehr, String activity) {
    Row row = dataSheet.nextRow();
    writeStringCell(row, 0, diplotype, false);
    writeStringCell(row, 1, phenotype);
    writeStringCell(row, 2, activity, false);
    writeStringCell(row, 3, ehr);
  }
  
  void writeInterpretation(String phenotype, String ehr, String interpreation) {
    Row row = getNoteSheet().nextRow();
    writeStringCell(row, 0, phenotype);
    writeStringCell(row, 1, ehr);
    writeStringCell(row, 2, interpreation, this.wrapStyle);
  }
  
  @Override
  String getFilename() {
    return String.format(NAME_TEMPLATE, this.gene);
  }

  private SheetWrapper getNoteSheet() {
    if (this.noteSheet == null) {
      this.noteSheet = findSheet(NOTES_SHEET_NAME);
      this.noteSheet.setColCount(3);
      Row headerRow = this.noteSheet.nextRow();
      writeHeaderCell(headerRow, 0, "Coded Genotype/Phenotype Summary");
      writeHeaderCell(headerRow, 1, "EHR Priority Result Notation");
      writeHeaderCell(headerRow, 2, "Consultation (Interpretation) Text Provided with Test Result");
    }
    return this.noteSheet;
  }
}
