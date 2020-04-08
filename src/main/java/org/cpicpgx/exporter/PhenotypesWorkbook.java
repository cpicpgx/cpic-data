package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

public class PhenotypesWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_TEMPLATE = "%s-Phenotypes.xlsx";
  private static final String SHEET_NAME = "Phenotype";

  private String gene;
  private SheetWrapper sheet;

  PhenotypesWorkbook(String gene) {
    super();
    this.gene = gene;
    this.sheet = findSheet(SHEET_NAME);

    this.sheet.setColCount(7);

    Row geneHeader = this.sheet.nextRow();
    writeHeaderCell(geneHeader, 0, "Gene: " + this.gene);

    Row columnHeaderRow = this.sheet.nextRow();
    writeHeaderCell(columnHeaderRow, 0, "Allele 1 Function");
    writeHeaderCell(columnHeaderRow, 1, "Allele 2 Function");
    writeHeaderCell(columnHeaderRow, 2, "Activity Value Allele 1");
    writeHeaderCell(columnHeaderRow, 3, "Activity Value Allele 2");
    writeHeaderCell(columnHeaderRow, 4, "Activity Score");
    writeHeaderCell(columnHeaderRow, 5, "Phenotype");
    writeHeaderCell(columnHeaderRow, 6, "Description");
  }

  void writePhenotype(
      String fn1,
      String fn2,
      String score1,
      String score2,
      String scoreTotal,
      String pheno,
      String description
  ) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, fn1, leftTextStyle);
    writeStringCell(row, 1, fn2, leftTextStyle);
    writeStringCell(row, 2, score1, rightNumberStyle);
    writeStringCell(row, 3, score2, rightNumberStyle);
    writeStringCell(row, 4, scoreTotal, rightNumberStyle);
    writeStringCell(row, 5, pheno, leftTextStyle);
    writeStringCell(row, 6, description, leftTextStyle);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.gene);
  }
}
