package org.cpicpgx.workbook;

import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook to list diplotype to phenotype mappings for a particular gene
 *
 * @author Ryan Whaley
 */
public class DiplotypeWorkbook extends AbstractWorkbook {
  private static final String NAME_TEMPLATE = "%s_Diplotype_Phenotype_Table.xlsx";
  private static final String SHEET_NAME = "Diplotypes";
  private final String gene;
  private final SheetWrapper dataSheet;

  public DiplotypeWorkbook(String gene) {
    super();
    this.gene = gene;
    
    this.dataSheet = findSheet(SHEET_NAME);
    this.dataSheet.setColCount(4);
    Row row = this.dataSheet.nextRow();
    
    writeHeaderCell(row, 0, String.format("%s Diplotype", this.gene));
    writeHeaderCell(row, 1, "Activity Score");
    writeHeaderCell(row, 2, "Coded Diplotype/Phenotype Summary");
    writeHeaderCell(row, 3, "EHR Priority Notation");
  }

  public void writeDiplotype(String diplotype, String phenotype, String ehr, String activity) {
    Row row = dataSheet.nextRow();
    writeStringCell(row, 0, diplotype, false);
    writeStringCell(row, 1, activity, false);
    writeStringCell(row, 2, phenotype);
    writeStringCell(row, 3, ehr);
  }

  @Override
  public String getFilename() {
    return String.format(NAME_TEMPLATE, this.gene);
  }
}
