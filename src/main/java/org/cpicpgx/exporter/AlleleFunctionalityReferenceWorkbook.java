package org.cpicpgx.exporter;

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
  private static final String FILE_NAME_PATTERN = "%s_allele_functionality_reference.xlsx";
  private String geneSymbol;
  private SheetWrapper sheet;

  
  AlleleFunctionalityReferenceWorkbook(String gene, Date modified) {
    super();
    this.geneSymbol = gene;
    
    this.sheet = findSheet(FUNCTION_SHEET_NAME);
    Row row = sheet.nextRow();
    
    writeStringCell(row, 0, String.format(CELL_PATTERN_GENE, this.geneSymbol));
    writeDateCell(row, modified);
    
    Row headerRow = sheet.nextRow();
    writeHeaderCell(headerRow, 0, "Allele");
    writeHeaderCell(headerRow, 1, "Allele Functional Status");
    writeHeaderCell(headerRow, 2, "PMID");
    writeHeaderCell(headerRow, 3, "Finding");
    writeHeaderCell(headerRow, 4, "Drug Substrate - in vitro");
    writeHeaderCell(headerRow, 5, "Drug Substrate - in vivo");
    sheet.setColCount(6);
    
    this.colIdx = 5;
  }
  
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
  
  void writeAlleleRow(String allele, String function, String pmid, String finding, List<String> inVitro, List<String> inVivo) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, allele);
    writeStringCell(row, 1, function);
    writeStringCell(row, 2, pmid, false);
    writeStringCell(row, 3, finding, false);
    writeStringCell(row, 4, String.join(", ", inVitro), false);
    writeStringCell(row, 5, String.join(", ", inVivo), false);
  }
}
