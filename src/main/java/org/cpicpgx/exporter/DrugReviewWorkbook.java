package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

/**
 * This is a workbook of data about Drugs and their related IDs in other systems
 *
 * @author Ryan Whaley
 */
class DrugReviewWorkbook extends AbstractWorkbook {
  
  private static final String SHEET_NAME = "CPIC Drug Review";
  private static final String FILE_NAME = "cpic_drug_review.xlsx";
  
  private SheetWrapper sheet;
  
  DrugReviewWorkbook() {
    super();
    this.sheet = findSheet(SHEET_NAME);
    this.sheet.setColCount(6);
    
    Row headerRow = sheet.nextRow();
    writeHeaderCell(headerRow, 0, "Name");
    writeHeaderCell(headerRow, 1, "ID");
    writeHeaderCell(headerRow, 2, "PharmGKB ID");
    writeHeaderCell(headerRow, 3, "RxNorm ID");
    writeHeaderCell(headerRow, 4, "DrugBank ID");
    writeHeaderCell(headerRow, 5, "ATC ID");
  }
  
  void writeDrug(String name, String id, String pgkb, String rxnorm, String drugbank, String[] atc) {
    Row drugRow = sheet.nextRow();
    writeStringCell(drugRow, 0, name, false);
    writeStringCell(drugRow, 1, id, false);
    writeLinkCell(drugRow, 2, pgkb,"https://www.pharmgkb.org/chemical/"+pgkb);
    writeLinkCell(drugRow, 3, rxnorm, "https://purl.bioontology.org/ontology/RXNORM/"+rxnorm);
    writeLinkCell(drugRow, 4, drugbank, "https://www.drugbank.ca/drugs/"+drugbank);
    
    if (atc != null && atc.length > 0) {
      for (int i = 0; i < atc.length; i++) {
        writeLinkCell(drugRow, 5+i, atc[i], "http://www.whocc.no/atc_ddd_index/?showdescription=yes&code="+atc[i]);
      }
    }
  }
  
  @Override
  String getFilename() {
    return FILE_NAME;
  }
}
