package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

/**
 * A workbook containing test alert language for gene phenotypes
 *
 * @author Ryan Whaley
 */
class TestAlertWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_TEMPLATE = "%s-Pre_and_Post_Test_Alerts.xlsx";
  
  private final String drug;
  private SheetWrapper sheet;

  TestAlertWorkbook(String drug) {
    super();
    this.drug = drug;
  }

  void writeSheet(String population, String[] genes) {
    this.sheet = findSheet(population);
    this.colIdx = 0;

    Row headerRow = this.sheet.nextRow();
    writeHeaderCell(headerRow, colIdx++, "Drug Ordered");
    for (String gene : genes) {
      writeHeaderCell(headerRow, colIdx++, gene + " Phenotype");
      writeHeaderCell(headerRow, colIdx++, gene + " Activity Score");
    }
    writeHeaderCell(headerRow, colIdx++, "CDS Context, Relative to Genetic Testing");
    writeHeaderCell(headerRow, colIdx++, "CDS Alert Text");

    this.sheet.setColCount(colIdx+1);
    Integer[] columnSizes = new Integer[colIdx+1];
    columnSizes[colIdx] = 15*256;
    this.sheet.setWidths(columnSizes);
  }
  
  void writeAlert(String[] genes, String context, String[] alertText, String drugName, Map<String,String> activity, Map<String,String> phenotype) {
    this.colIdx = 0;
    Row row = this.sheet.nextRow();
    writeStringCell(row, colIdx++, drugName);
    for (String gene : genes) {
      writeStringCell(row, colIdx++, phenotype.get(gene), false);
      writeStringCell(row, colIdx++, activity.get(gene), false);
    }
    writeStringCell(row, colIdx++, context);
    writeStringCell(row, colIdx++, String.join("\n\n", alertText), wrapStyle);
  }
  
  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug);
  }
}
