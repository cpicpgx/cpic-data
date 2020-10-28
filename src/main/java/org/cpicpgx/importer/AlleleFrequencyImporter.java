package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class to read all excel files in the given directory and store the allele frequency information found in them.
 * 
 * Excel file names are expected to be snake_cased and have the gene symbol as the first word in the filename.
 *
 * @author Ryan Whaley
 */
public class AlleleFrequencyImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern SHEET_ALLELE_NAME = Pattern.compile("Allele frequency( by group)?");
  private static final Pattern SHEET_DIPLO_NAME = Pattern.compile("Diplotype frequency( by group)?");
  private static final Pattern SHEET_PHENO_NAME = Pattern.compile("Phenotype frequency( by group)?");
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
    workbook.currentSheetIs("References");
    try (FrequencyProcessor frequencyProcessor = new FrequencyProcessor(gene, workbook.getRow(0))) {
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        try {
          frequencyProcessor.insertPopulation(workbook.getRow(i));
        } catch (Exception ex) {
          throw new RuntimeException("Error parsing row " + (i+1), ex);
        }
      }

      writeNotes(gene, workbook.getNotes());

      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        if (row.hasNoText(0) ^ row.hasNoText(1)) {
          throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
        }
        else if (row.hasNoText(0)) continue;
        
        Date date = row.getDate(0);
        String note = row.getNullableText(1);
        frequencyProcessor.insertHistory(date, note);
      }

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

      // START handling allele frequencies
      try {
        workbook.findSheet(SHEET_ALLELE_NAME);

        Map<Integer,String> colIdxToPopulationMap = new LinkedHashMap<>();
        RowWrapper headerRow = workbook.getRow(1);
        for (int i=1; i < headerRow.getLastCellNum(); i++) {
          if (headerRow.hasNoText(i)) continue;
          colIdxToPopulationMap.put(i, headerRow.getText(i).replaceAll(" Allele Frequency", ""));
        }

        for (int i = 2; i <= workbook.currentSheet.getLastRowNum(); i++) {
          RowWrapper row = workbook.getRow(i);
          if (row.hasNoText(0)) break;

          String rawAlleleName = StringUtils.stripToNull(row.getText(0));
          rawAlleleName = normalizeGeneText(gene, rawAlleleName);
          if (!frequencyProcessor.isValidAllele(rawAlleleName)) {
            sf_logger.warn("Allele not found for gene {}: [{}]", gene, rawAlleleName);
            continue;
          }

          JsonObject frequencyMap = new JsonObject();
          for (Integer colIdx : colIdxToPopulationMap.keySet()) {
            try {
              Double freq = row.getNullableDouble(colIdx);
              frequencyMap.addProperty(colIdxToPopulationMap.get(colIdx), freq);
            } catch (NumberFormatException ex) {
              sf_logger.warn("Allele frequency value for {} not in proper format [{}]: {}", rawAlleleName, workbook.currentSheet.getSheetName(), ex.getMessage());
            }
          }

          frequencyProcessor.updateAlleleFrequency(rawAlleleName, frequencyMap);
        }
      } catch (InvalidParameterException ex) {
        sf_logger.info("no allele frequency sheet");
      }

      // START handling diplotype frequencies
      try {
        frequencyProcessor.startTransaction();
        workbook.findSheet(SHEET_DIPLO_NAME);

        int rowIdx =0;
        Map<Integer,String> colIdxToPopulationMap = new LinkedHashMap<>();
        for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
          RowWrapper headerRow = workbook.getRow(rowIdx);
          if (headerRow.row != null && (headerRow.hasNoText(0) || headerRow.getText(0).equals("Diplotype"))) {
            for (int i=1; i < headerRow.getLastCellNum(); i++) {
              if (headerRow.hasNoText(i)) break;
              colIdxToPopulationMap.put(i, headerRow.getText(i).replaceAll(" Allele Frequency", ""));
            }
            if (colIdxToPopulationMap.size()>0) break;
          }
        }

        rowIdx += 1;
        for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
          RowWrapper row = workbook.getRow(rowIdx);
          if (row.hasNoText(0)) break;

          String rawDiplotype = StringUtils.stripToNull(row.getText(0));
          JsonObject diplotypeJson = makeDiplotyepJson(gene, rawDiplotype);

          JsonObject frequencyMap = new JsonObject();
          for (Integer colIdx : colIdxToPopulationMap.keySet()) {
            frequencyMap.addProperty(colIdxToPopulationMap.get(colIdx), row.getNullableDouble(colIdx));
          }

          frequencyProcessor.updateDiplotypeFrequency(diplotypeJson, frequencyMap);
        }
        frequencyProcessor.endTransaction();
      } catch (RuntimeException|NotFoundException ex) {
        sf_logger.info(ex.getMessage());
        frequencyProcessor.rollbackTransaction();
      }

      // START handling phenotype frequencies
      try {
        frequencyProcessor.startTransaction();
        workbook.findSheet(SHEET_PHENO_NAME);

        int rowIdx =0;
        Map<Integer,String> colIdxToPopulationMap = new LinkedHashMap<>();
        for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
          RowWrapper headerRow = workbook.getRow(rowIdx);
          if (headerRow.row != null && (headerRow.hasNoText(0) || headerRow.getText(0).equals("Phenotype") || headerRow.getText(0).equals("Activity Score"))) {
            for (int i=1; i < headerRow.getLastCellNum(); i++) {
              if (headerRow.hasNoText(i)) break;
              colIdxToPopulationMap.put(i, headerRow.getText(i).replaceAll(" Allele Frequency", ""));
            }
            if (colIdxToPopulationMap.size()>0) break;
          }
        }

        rowIdx += 1;
        for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
          RowWrapper row = workbook.getRow(rowIdx);
          if (row.hasNoText(0)) break;

          String resultValue;
          if (frequencyProcessor.getLookupMethod() == LookupMethod.PHENOTYPE) {
            resultValue = frequencyProcessor.validPhenotype(gene, normalizeGeneText(gene, row.getText(0)));
          } else {
            resultValue = frequencyProcessor.validActivityScore(gene, normalizeActivityScore(row.getNullableText(0)));
          }

          JsonObject frequencyMap = new JsonObject();
          for (Integer colIdx : colIdxToPopulationMap.keySet()) {
            frequencyMap.addProperty(colIdxToPopulationMap.get(colIdx), row.getNullableDouble(colIdx));
          }

          frequencyProcessor.updateResultFrequency(resultValue, frequencyMap);
        }
        frequencyProcessor.endTransaction();
      } catch (RuntimeException|NotFoundException ex) {
        sf_logger.info(ex.getMessage());
        frequencyProcessor.rollbackTransaction();
      }

      sf_logger.debug("Successfully parsed " + gene + " frequencies");
    }
  }

  JsonObject makeDiplotyepJson(String gene, String rawDiplotype) {
    String diplotype = StringUtils.stripToNull(rawDiplotype);
    if (diplotype == null) {
      return null;
    }
    else if (diplotype.contains("/")) {
      String[] alleles = diplotype.replaceAll(">=", "≥").split("/");
      JsonObject alleleJson = new JsonObject();
      if (alleles[0].equals(alleles[1])) {
        alleleJson.addProperty(alleles[0], 2);
      } else {
        alleleJson.addProperty(alleles[0], 1);
        alleleJson.addProperty(alleles[1], 1);
      }

      JsonObject geneJson = new JsonObject();
      geneJson.add(gene, alleleJson);
      return geneJson;
    }
    else {
      JsonObject alleleJson = new JsonObject();
      alleleJson.addProperty(diplotype, 1);
      JsonObject geneJson = new JsonObject();
      geneJson.add(gene, alleleJson);
      return geneJson;
    }
  }
}
