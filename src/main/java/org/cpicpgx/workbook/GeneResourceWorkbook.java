package org.cpicpgx.workbook;

import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook to list IDs of a gene in external resources (e.g. NCBI, HGNC)
 *
 * @author Ryan Whaley
 */
public class GeneResourceWorkbook extends AbstractWorkbook {
  private static final String NAME_TEMPLATE = "%s-Gene_Resource_Mappings.xlsx";
  private static final String SHEET_NAME = "mapping";
  private String gene;
  private SheetWrapper sheet;

  public GeneResourceWorkbook(String gene) {
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

  public void writeIds(String hgncId, String ncbiId, String ensemblId, String clinpgxid) {
    writeMapping("HGNC", "Symbol", this.gene);
    writeMapping("HGNC", "HGNC ID", hgncId);
    writeMapping("NCBI", "Gene ID", ncbiId);
    writeMapping("Ensembl", "Ensembl ID", ensemblId);
    writeMapping("ClinPGx", "ClinPGx ID", clinpgxid);
  }
  
  void writeMapping(String source, String type, String code) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, this.gene, false);
    writeStringCell(row, 1, source, false);
    writeStringCell(row, 2, type, false);
    writeStringCell(row, 3, code, false);
  }

  public String getFilename() {
    return String.format(NAME_TEMPLATE, this.gene);
  }
}
