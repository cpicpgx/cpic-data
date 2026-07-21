package org.cpicpgx.importer;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.FrequencyGenerator;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.cpicpgx.workbook.AbstractWorkbook;
import org.cpicpgx.workbook.FrequencyWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.util.*;

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
      "update allele set frequency=null where frequency is not null",
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
    try (FrequencyProcessor frequencyProcessor = new FrequencyProcessor(gene)) {

      if (workbook.hasSheet(FrequencyWorkbook.SHEET_NAME_REFERENCES)) {
        sf_logger.debug("Processing reference data for frequencies");
        processWithReferences(workbook, frequencyProcessor, gene);
      } else {
        sf_logger.debug("Processing direct frequencies");

        sf_logger.debug("Processing alleles");

        List<GroupPopulationFrequency> frequencies = processAllelesSheet(workbook);
        frequencyProcessor.storeAlleleFrequencies(frequencies);

        frequencies = processDiplotypesSheet(workbook);
        frequencyProcessor.storeDiplotypeFrequencies(frequencies);

        frequencies = processPhenotypesSheet(workbook);
        frequencyProcessor.storePhenotypeFrequencies(frequencies);
      }

      // START processing Change log sheet
      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      processChangeLog(frequencyProcessor, workbook, gene);
      // END processing Change log sheet

      // START processing Methods sheet
      workbook.currentSheetIs(FrequencyWorkbook.SHEET_NAME_METHODS);
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

      // START processing notes sheet
      writeNotes(gene, workbook.getNotes());
      // END processing notes sheet
    }
    sf_logger.debug("Successfully parsed " + gene + " frequencies");
  }

  private void processWithReferences(WorkbookWrapper workbook, FrequencyProcessor frequencyProcessor, String gene) throws Exception {
    Preconditions.checkArgument(workbook.hasSheet(FrequencyWorkbook.SHEET_NAME_REFERENCES));

    workbook.currentSheetIs(FrequencyWorkbook.SHEET_NAME_REFERENCES);

    frequencyProcessor.parseReferencesHeader(workbook.getRow(0));
    for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
      try {
        frequencyProcessor.insertPopulation(workbook.getRow(i));
      } catch (Exception ex) {
        throw new RuntimeException("Error parsing row " + (i+1), ex);
      }
    }

    // generate the rest of the frequencies based on what's in the References tab
    FrequencyGenerator generator = new FrequencyGenerator(gene);
    generator.calculate();

    sf_logger.debug("Successfully calculated " + gene + " diplotype/phenotype frequencies");
  }

  List<GroupPopulationFrequency> processAllelesSheet(WorkbookWrapper workbook) {
    return processGroupSheet(workbook, FrequencyWorkbook.SHEET_NAME_ALLELE);
  }

  List<GroupPopulationFrequency> processDiplotypesSheet(WorkbookWrapper workbook) {
    return processGroupSheet(workbook, FrequencyWorkbook.SHEET_DIPLOTYPE);
  }

  List<GroupPopulationFrequency> processPhenotypesSheet(WorkbookWrapper workbook) {
    return processGroupSheet(workbook, FrequencyWorkbook.SHEET_PHENOTYPE);
  }

  List<GroupPopulationFrequency> processGroupSheet(WorkbookWrapper workbook, String sheetName) {
    List<GroupPopulationFrequency> groupPopulationFrequencies = new ArrayList<>();
    sf_logger.debug("Processing allele sheet");
    if (!workbook.hasSheet(sheetName)) {
      sf_logger.debug("Nothing to process");
      return groupPopulationFrequencies;
    }

    workbook.currentSheetIs(sheetName);
    final int idxAlleleName = 0;
    final Map<String, Integer> popToIdxMap = new HashMap<>();
    RowWrapper headerRow = workbook.getRow(1);
    for (int i = 1; i < headerRow.row.getLastCellNum(); i++) {
      String cellText = headerRow.getNullableText(i);
      if (StringUtils.isNotBlank(cellText)) {
        popToIdxMap.put(cellText, i);
      }
    }
    for (int i = 2; i < workbook.currentSheet.getLastRowNum(); i++) {
      RowWrapper alleleRow = workbook.getRow(i);
      String alleleName = alleleRow.getNullableText(idxAlleleName);
      if (StringUtils.isBlank(alleleName)) {
        continue;
      }

      GroupPopulationFrequency groupPopulationFrequency = new GroupPopulationFrequency(alleleName);
      groupPopulationFrequencies.add(groupPopulationFrequency);
      for (Map.Entry<String, Integer> entry : popToIdxMap.entrySet()) {
        String popName = entry.getKey();
        Double popFreq =  alleleRow.getNullableDouble(entry.getValue());

        groupPopulationFrequency.setPopFreq(popName, popFreq);
        sf_logger.debug("Population frequency for {}: {} is {}", alleleName, popName, popFreq);
      }
    }
    return groupPopulationFrequencies;
  }

  static class GroupPopulationFrequency {
    final String groupName;
    final Map<String, Double> popNameToFreqMap = new HashMap<>();

    GroupPopulationFrequency(String groupName) {
      this.groupName = groupName;
    }

    void setPopFreq(String popName, Double popFreq) {
      popNameToFreqMap.put(popName, popFreq);
    }
  }
}

