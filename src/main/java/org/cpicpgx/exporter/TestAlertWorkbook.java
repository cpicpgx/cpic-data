package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.cpicpgx.db.LookupMethod;

import java.util.Map;

/**
 * A workbook containing test alert language for gene phenotypes
 *
 * @author Ryan Whaley
 */
class TestAlertWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_TEMPLATE = "%s_Pre_and_Post_Test_Alerts.xlsx";
  
  private final String drug;
  private SheetWrapper sheet;

  TestAlertWorkbook(String drug) {
    super();
    this.drug = drug;
  }

  void writeSheet(String population, Map<String, LookupMethod> genes) {
    this.sheet = findSheet(population);
    this.colIdx = 0;

    Row headerRow = this.sheet.nextRow();
    writeHeaderCell(headerRow, colIdx++, "Drug Ordered");
    for (String gene : genes.keySet()) {
      switch (genes.get(gene)) {
        case PHENOTYPE:
          writeHeaderCell(headerRow, colIdx++, gene + " Phenotype");
          break;
        case ACTIVITY_SCORE:
          writeHeaderCell(headerRow, colIdx++, gene + " Activity Score");
          break;
        case ALLELE_STATUS:
          writeHeaderCell(headerRow, colIdx++, gene + " Allele");
          break;
        default:
          throw new RuntimeException("Lookup method not implemented");
      }
    }
    writeHeaderCell(headerRow, colIdx++, "CDS Context, Relative to Genetic Testing");
    writeHeaderCell(headerRow, colIdx++, "CDS Alert Text");

    this.sheet.setColCount(colIdx+1);
    Integer[] columnSizes = new Integer[colIdx+1];
    columnSizes[colIdx] = 15*256;
    this.sheet.setWidths(columnSizes);
  }
  
  void writeAlert(Map<String,LookupMethod> genes, String context, String[] alertText, String drugName, Map<String,String> activity, Map<String,String> phenotype, Map<String,String> alleleStatus) {
    this.colIdx = 0;
    Row row = this.sheet.nextRow();
    writeStringCell(row, colIdx++, drugName);
    for (String gene : genes.keySet()) {
      switch (genes.get(gene)) {
        case PHENOTYPE:
          writeStringCell(row, colIdx++, phenotype.get(gene), false);
          break;
        case ACTIVITY_SCORE:
          writeStringCell(row, colIdx++, activity.get(gene), false);
          break;
        case ALLELE_STATUS:
          writeStringCell(row, colIdx++, alleleStatus.get(gene), false);
          break;
        default:
          throw new RuntimeException("Lookup method not implemented");
      }
    }
    writeStringCell(row, colIdx++, context);
    writeStringCell(row, colIdx++, String.join("\n\n", alertText), wrapStyle);
  }
  
  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug.replaceAll("/", "_"));
  }
}
