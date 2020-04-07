package org.cpicpgx.exporter;

import com.google.common.base.Joiner;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;
import java.util.List;

/**
 * A workbook of allele functionality information
 *
 * @author Ryan Whaley
 */
class AlleleFunctionalityReferenceWorkbook extends AbstractWorkbook {
  private static final String FUNCTION_SHEET_NAME = "Allele Function";
  private static final String CELL_PATTERN_GENE = "Gene: %s";
  private static final String FILE_NAME_PATTERN = "%s-Allele_Functionality_Reference.xlsx";
  private String geneSymbol;
  private SheetWrapper sheet;

  
  AlleleFunctionalityReferenceWorkbook(String gene) {
    super();
    this.geneSymbol = gene;
    
    this.sheet = findSheet(FUNCTION_SHEET_NAME);
    Row row = sheet.nextRow();
    
    writeBoldStringCell(row, 0, String.format(CELL_PATTERN_GENE, this.geneSymbol));
    
    Row headerRow = sheet.nextRow();
    writeHeaderCell(headerRow, 0, "Allele/cDNA/rsID");
    writeHeaderCell(headerRow, 1, "Activity Score (Optional)");
    writeHeaderCell(headerRow, 2, "Allele Functional Status (Optional)");
    writeHeaderCell(headerRow, 3, "Allele Clinical Functional Status (Required)");
    writeHeaderCell(headerRow, 4, "Allele Clinical Function Substrate Specificity (Optional)");
    writeHeaderCell(headerRow, 5, "PMID (Optional)");
    writeHeaderCell(headerRow, 6, "Strength of Evidence (Optional)");
    writeHeaderCell(headerRow, 7, "Findings (Optional)");
    writeHeaderCell(headerRow, 8, "Comments");
    sheet.setColCount(9);
    
    this.colIdx = 8;
  }
  
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
  
  void writeAlleleRow(String allele, String activity, String function, String clinFunction, String clinSubstrate, String[] citations, String strength, String finding, String comments) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, allele, false);
    writeStringCell(row, 1, activity, false);
    writeStringCell(row, 2, function, false);
    writeStringCell(row, 3, clinFunction, false);
    writeStringCell(row, 4, clinSubstrate, false);
    writeStringCell(row, 5, Joiner.on(", ").join(citations), false);
    writeStringCell(row, 6, strength, false);
    writeStringCell(row, 7, finding, false);
    writeStringCell(row, 8, comments, false);
  }
  
  private int nHistory = 0;
  void writeHistory(Date date, String note) {
    if (nHistory == 0) {
      this.sheet = findSheet(HISTORY_SHEET_NAME);
      Row headerRow = this.sheet.nextRow();
      writeHeaderCell(headerRow, 0, "Date");
      writeHeaderCell(headerRow, 1, "Note");
    }
    Row row = this.sheet.nextRow();
    writeDateCell(row, 0, date);
    writeStringCell(row, 1, note, false);
    
    nHistory += 1;
  }
}
