package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.cpicpgx.db.LookupMethod;

/**
 * A workbook for example CDS language for a given Gene
 *
 * @author Ryan Whaley
 */
class GeneCdsWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_PATTERN = "%s_CDS.xlsx";
  private static final String SHEET_NAME = "CDS";
  private final String geneSymbol;
  private final SheetWrapper sheetWrapper;

  GeneCdsWorkbook(String geneSymbol, LookupMethod lookupMethod) {
    super();
    this.geneSymbol = geneSymbol;
    this.sheetWrapper = findSheet(SHEET_NAME);
    
    Row geneRow = sheetWrapper.nextRow();
    writeHeaderCell(geneRow, 0, "Gene: " + geneSymbol);
    Row headerRow = sheetWrapper.nextRow();
    if (lookupMethod == LookupMethod.ALLELE_STATUS) {
      writeHeaderCell(headerRow, 0, this.geneSymbol + " Allele Status");
    } else {
      writeHeaderCell(headerRow, 0, this.geneSymbol + " Phenotype");
    }
    writeHeaderCell(headerRow, 1, "Activity Score");
    writeHeaderCell(headerRow, 2, "EHR Priority Result Notation");
    writeHeaderCell(headerRow, 3, "Consultation (Interpretation) Text Provided with Test Result");
    sheetWrapper.setColCount(4);
    sheetWrapper.setWidths(new Integer[]{50*256, 50*256, 50*256, 80*256});
  }
  
  @Override
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
  
  void writeConsultation(String phenotype, String priority, String consultation, String activityScore) {
    Row headerRow = sheetWrapper.nextRow();
    writeStringCell(headerRow, 0, phenotype);
    writeStringCell(headerRow, 1, activityScore);
    writeStringCell(headerRow, 2, priority);
    writeStringCell(headerRow, 3, consultation, wrapStyle);
  }
}
