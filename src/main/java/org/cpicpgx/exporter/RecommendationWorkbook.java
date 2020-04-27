package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;

import java.util.Map;
import java.util.Set;

/**
 * A representation of a Recommendation workbook
 *
 * @author Ryan Whaley
 */
class RecommendationWorkbook extends AbstractWorkbook {
  
  private static final String FILE_NAME_TEMPLATE = "%s-Recommendations.xlsx";

  private SheetWrapper sheet;
  private final String drug;
  private final Set<String> genes;
  
  RecommendationWorkbook(String drug, Set<String> genes) {
    super();
    this.drug = drug;
    this.genes = genes;
  }

  void setupSheet(String population) {
    this.sheet = findSheet(population);

    int colIdx = 0;
    Row headerRow = this.sheet.nextRow();
    for (String gene : genes) {
      writeHeaderCell(headerRow, colIdx++, gene + " Phenotype");
      writeHeaderCell(headerRow, colIdx++, gene + " Activity Score");
    }
    for (String gene : genes) {
      writeHeaderCell(headerRow, colIdx++, gene + " Implication for Phenotypic Measures");
    }
    writeHeaderCell(headerRow, colIdx++, "Therapeutic Recommendation");
    writeHeaderCell(headerRow, colIdx++, "Classification of Recommendation");
    writeHeaderCell(headerRow, colIdx++, "Comments");
    this.sheet.setColCount(colIdx);
  }
  
  void writeRec(
      Map<String,String> phenotypeMap,
      Map<String,String> activityMap,
      Map<String,String> implicationMap,
      String rec,
      String classification,
      String comments
  ) {
    Row row = this.sheet.nextRow();
    int colIdx = 0;
    for (String gene : this.genes) {
      writeStringCell(row, colIdx++, phenotypeMap.get(gene), false);
      writeStringCell(row, colIdx++, activityMap.get(gene), this.wrapStyle);
    }
    for (String gene : this.genes) {
      writeStringCell(row, colIdx++, implicationMap.get(gene), false);
    }
    writeStringCell(row, colIdx++, rec, false);
    writeStringCell(row, colIdx++, classification, false);
    writeStringCell(row, colIdx, comments, false);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug);
  }
}
