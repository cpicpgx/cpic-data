package org.cpicpgx.importer;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

/**
 * Class to read all excel files in the given directory and store the allele frequency information found in them.
 * 
 * Excel file names are expected to be snake_cased and have the gene symbol as the first word in the filename.
 *
 * @author Ryan Whaley
 */
public class AlleleFrequencyImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  public static void main(String[] args) {
    try {
      AlleleFrequencyImporter processor = new AlleleFrequencyImporter();
      processor.parseArgs(args);
      processor.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }
  
  private AlleleFrequencyImporter() { }

  /**
   * Constructor
   * @param directory an existing directory containing Excel .xlsx files
   */
  public AlleleFrequencyImporter(Path directory) {
    this.setDirectory(directory);
  }

  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) {
    sf_logger.info("Reading {}", workbook.getFileName());
    String[] nameParts = workbook.getFileName().split("_");
    processAlleles(workbook, nameParts[0]);
  }

  /**
   * Finds the sheet with allele data and iterates through the rows with data.
   * The session is auto-committed so no explict commit is done here.
   * @param workbook The workbook to read
   * @param gene The symbol of the gene the alleles in this workbook are for
   */
  private void processAlleles(WorkbookWrapper workbook, String gene) {
    workbook.currentSheetIs("References");
    
    try (FrequencyProcessor frequencyProcessor = new FrequencyProcessor(gene, workbook.getRow(0))) {
      for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
        try {
          frequencyProcessor.insertPopulation(workbook.getRow(i));
        } catch (Exception ex) {
          throw new RuntimeException("Error parsing row " + (i+1), ex);
        }
      }
      sf_logger.info("Successfully parsed " + gene + " frequencies");
    } catch (Exception ex) {
      sf_logger.error("Error saving to DB", ex);
    }
  }
}
