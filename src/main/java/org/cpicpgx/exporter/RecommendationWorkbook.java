package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.cpicpgx.db.LookupMethod;

import java.util.Map;

/**
 * A representation of a Recommendation workbook
 *
 * @author Ryan Whaley
 */
class RecommendationWorkbook extends AbstractWorkbook {
  
  private static final String FILE_NAME_TEMPLATE = "%s recommendation.xlsx";

  private SheetWrapper sheet;
  private final String drug;
  private final Map<String, LookupMethod> genes;
  
  RecommendationWorkbook(String drug, Map<String, LookupMethod> geneMap) {
    super();
    this.drug = drug;
    this.genes = geneMap;
  }

  void setupSheet(String population) {
    this.sheet = findSheet("population " + population);

    int colIdx = 0;
    Row headerRow = this.sheet.nextRow();
    for (String gene : genes.keySet()) {
      switch (genes.get(gene)) {
        case ACTIVITY_SCORE:
          writeHeaderCell(headerRow, colIdx++, gene + " Activity Score");
        case PHENOTYPE:
          writeHeaderCell(headerRow, colIdx++, gene + " Phenotype");
          break;
        case ALLELE_STATUS:
          writeHeaderCell(headerRow, colIdx++, gene + " Allele Status");
          break;
        default:
          throw new RuntimeException("Lookup method not implemented");
      }
    }
    for (String gene : genes.keySet()) {
      writeHeaderCell(headerRow, colIdx++, gene + " Implications for Phenotypic Measures");
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
      Map<String,String> alleleStatusMap,
      String rec,
      String classification,
      String comments
  ) {
    Row row = this.sheet.nextRow();
    int colIdx = 0;
    for (String gene : this.genes.keySet()) {
      switch(this.genes.get(gene)) {
        case ACTIVITY_SCORE:
          writeStringCell(row, colIdx++, activityMap.get(gene), this.wrapStyle);
        case PHENOTYPE:
          writeStringCell(row, colIdx++, phenotypeMap.get(gene), this.wrapStyle);
          break;
        case ALLELE_STATUS:
          writeStringCell(row, colIdx++, alleleStatusMap.get(gene), this.wrapStyle);
          break;
        default:
          throw new RuntimeException("Lookup method not implemented");
      }
    }
    for (String gene : this.genes.keySet()) {
      writeStringCell(row, colIdx++, implicationMap.get(gene), false);
    }
    writeStringCell(row, colIdx++, rec, false);
    writeStringCell(row, colIdx++, classification, false);
    writeStringCell(row, colIdx, comments, false);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_TEMPLATE, this.drug.replaceAll("/", "_"));
  }
}
