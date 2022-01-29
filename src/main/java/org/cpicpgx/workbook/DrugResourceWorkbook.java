package org.cpicpgx.workbook;

import com.google.common.base.Joiner;
import org.apache.poi.ss.usermodel.Row;

/**
 * A workbook to list the IDs for a drug in external resources (e.g. ATC, RxNorm)
 *
 * @author Ryan Whaley
 */
public class DrugResourceWorkbook extends AbstractWorkbook {
  private static final String NAME_TEMPLATE = "%s-Drug_Resource_Mappings.xlsx";
  private static final String SHEET_NAME = "mapping";
  private final SheetWrapper sheet;
  private final String drug;

  public DrugResourceWorkbook(String drugName) {
    super();
    this.drug = drugName;

    this.sheet = findSheet(SHEET_NAME);

    Row row = this.sheet.nextRow();
    writeHeaderCell(row, 0, "Drug or Ingredient");
    writeHeaderCell(row, 1, "Source");
    writeHeaderCell(row, 2, "Code Type");
    writeHeaderCell(row, 3, "Code");

    this.sheet.setColCount(4);
  }

  public void writeMapping(String rxnorm, String drugbank, String[] atc, String pharmgkb) {
    writeRow("RxNorm", "RxCUI", rxnorm);
    writeRow("DrugBank", "Accession Number", drugbank);
    writeRow("ATC", "ATC Code", Joiner.on(";").join(atc));
    writeRow("PharmGKB", "PharmGKB Accession ID", pharmgkb);
  }

  void writeRow(String system, String idType, String idValue) {
    Row row = this.sheet.nextRow();
    writeStringCell(row, 0, this.drug, false);
    writeStringCell(row, 1, system, false);
    writeStringCell(row, 2, idType, false);
    writeStringCell(row, 3, idValue, false);
  }

  @Override
  public String getFilename() {
    return String.format(NAME_TEMPLATE, this.drug.replaceAll("/", "_"));
  }
}
