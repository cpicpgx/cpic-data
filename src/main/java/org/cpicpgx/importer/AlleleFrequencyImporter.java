package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.FrequencyGenerator;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.cpicpgx.workbook.AbstractWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.util.StringJoiner;

/**
 * Class to read all excel files in the given directory and store the allele frequency information found in them.
 * 
 * Excel file names are expected to be snake_cased and have the gene symbol as the first word in the filename.
 *
 * @author Ryan Whaley
 */
public class AlleleFrequencyImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  //language=PostgreSQL
  private static final String[] sf_deleteStatements = new String[]{
      "delete from change_log where type='" + FileType.FREQUENCY.name() + "'",
      "delete from file_note where type='" + FileType.FREQUENCY.name() + "'",
      "delete from allele_frequency where alleleid is not null",
      "delete from population where id is not null",
      "update gene_result set frequency=null where frequency is not null",
      "update gene_result_diplotype set frequency=null where frequency is not null",
  };

  public static void main(String[] args) {
    rebuild(new AlleleFrequencyImporter(), args);
  }
  
  public AlleleFrequencyImporter() { }

  @Override
  public FileType getFileType() {
    return FileType.FREQUENCY;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return Constants.EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    String[] nameParts = workbook.getFileName().split("_");
    processAlleles(workbook, nameParts[0]);
  }

  /**
   * Finds the sheet with allele data and iterates through the rows with data.
   * The session is auto-committed so no explict commit is done here.
   * @param workbook The workbook to read
   * @param gene The symbol of the gene the alleles in this workbook are for
   */
  private void processAlleles(WorkbookWrapper workbook, String gene) throws Exception {
    workbook.currentSheetIs("References");
    try (FrequencyProcessor frequencyProcessor = new FrequencyProcessor(gene, workbook.getRow(0))) {

      // START processing References sheet
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        try {
          frequencyProcessor.insertPopulation(workbook.getRow(i));
        } catch (Exception ex) {
          throw new RuntimeException("Error parsing row " + (i+1), ex);
        }
      }
      writeNotes(gene, workbook.getNotes());
      // END processing References sheet

      // START processing Change log sheet
      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      processChangeLog(frequencyProcessor, workbook, gene);
      // END processing Change log sheet

      // START processing Methods sheet
      boolean foundSheet = false;
      try {
        workbook.currentSheetIs("Methods and caveats");
        foundSheet = true;
      } catch (InvalidParameterException ex) {
        // drop the exception
      }
      try {
        workbook.currentSheetIs("Methods");
        foundSheet = true;
      } catch (InvalidParameterException ex) {
        // drop the exception
      }
      if (!foundSheet) {
        throw new RuntimeException("Could not find methods sheet");
      }
      StringJoiner methodsText = new StringJoiner("\n");
      for (int i = 0; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        if (row.hasNoText(0)) {
          methodsText.add("");
        } else {
          methodsText.add(StringUtils.defaultIfBlank(row.getNullableText(0), ""));
        }
      }
      frequencyProcessor.updateMethods(methodsText.toString());
      // END processing Methods sheet

      sf_logger.debug("Successfully parsed " + gene + " frequencies");

      FrequencyGenerator generator = new FrequencyGenerator(gene);
      generator.calculate();

      sf_logger.debug("Successfully calculated " + gene + " diplotype/phenotype frequencies");
    }
  }
}
