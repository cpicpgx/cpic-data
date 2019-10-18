package org.cpicpgx.importer;

import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Class to read all excel files in the given directory and store the allele frequency information found in them.
 * 
 * Excel file names are expected to be snake_cased and have the gene symbol as the first word in the filename.
 *
 * @author Ryan Whaley
 */
public class AlleleFrequencyImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String[] sf_deleteStatements = new String[]{
      "delete from allele_frequency",
      "delete from population"
  };
  private static final String DEFAULT_DIRECTORY = "frequency_table";
  
  public static void main(String[] args) {
    rebuild(new AlleleFrequencyImporter(), args);
  }
  
  public AlleleFrequencyImporter() { }

  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.FREQUENCIES;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) {
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
      addImportHistory(workbook.getFileName());
      sf_logger.info("Successfully parsed " + gene + " frequencies");
    } catch (Exception ex) {
      sf_logger.error("Error saving to DB", ex);
    }
  }
}
