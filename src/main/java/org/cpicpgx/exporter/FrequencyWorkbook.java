package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.*;

/**
 * This is a sheet that displays allele frequencies in different aggregate views in different tabs
 *
 * @author Ryan Whaley
 */
class FrequencyWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_PATTERN = "%s-Frequency.xlsx";

  private static final String SHEET_NAME_METHODS = "Methods and Caveats";
  private static final String SHEET_NAME_ALLELE = "Allele frequency by race";
  private static final String SHEET_NAME_REFERENCES = "References";
  
  private static final String[] REFERENCE_COLUMNS = new String[]{
      "Authors", "Year", "PMID", "Major ethnicity", "Population", "Add'l population info", "Subject type", "N subjects genotyped"
  };

  private static ResourceBundle resources = ResourceBundle.getBundle("frequencyExport");
  
  private String geneSymbol;
  private SheetWrapper sheetMethods;
  private SheetWrapper sheetAllele;
  private SheetWrapper sheetReferences;
  private SheetWrapper sheetChanges;
  private Set<String> ethnicities = new TreeSet<>();
  
  FrequencyWorkbook(String geneSymbol) {
    super();

    if (StringUtils.isBlank(geneSymbol)) {
      throw new IllegalArgumentException("Must supply a gene");
    }
    this.geneSymbol = geneSymbol;
    
    sheetMethods = findSheet(SHEET_NAME_METHODS);
    sheetAllele = findSheet(SHEET_NAME_ALLELE);
    sheetReferences = findSheet(SHEET_NAME_REFERENCES);
    sheetChanges = findSheet(HISTORY_SHEET_NAME);
    
    writeMethods();
    
  }
  
  private void writeMethods() {
    sheetMethods.setColCount(1);
    sheetMethods.setWidths(new Integer[]{100*256});
    Row row = sheetMethods.nextRow();
    writeHeaderCell(row, 0, resources.getString("methods.title"));
    sheetMethods.nextRow();
    row = sheetMethods.nextRow();
    writeStringCell(row, 0, resources.getString("methods.text"), this.wrapStyle);
    sheetMethods.nextRow();
    row = sheetMethods.nextRow();
    writeHeaderCell(row, 0, resources.getString("cavaets.title"));
    sheetMethods.nextRow();
    row = sheetMethods.nextRow();
    writeBoldStringCell(row, 0, resources.getString("cavaets.sampling.title"));
    row = sheetMethods.nextRow();
    writeStringCell(row, 0, resources.getString("cavaets.sampling.text"), this.wrapStyle);
    sheetMethods.nextRow();
    row = sheetMethods.nextRow();
    writeBoldStringCell(row, 0, resources.getString("cavaets.star1.title"));
    row = sheetMethods.nextRow();
    writeStringCell(row, 0, resources.getString("cavaets.star1.text"), this.wrapStyle);
    sheetMethods.nextRow();
    row = sheetMethods.nextRow();
    writeBoldStringCell(row, 0, resources.getString("cavaets.dippheno.title"));
    row = sheetMethods.nextRow();
    writeStringCell(row, 0, resources.getString("cavaets.dippheno.text"), this.wrapStyle);
    sheetMethods.nextRow();
    row = sheetMethods.nextRow();
    writeBoldStringCell(row, 0, resources.getString("cavaets.ethnicity.title"));
    row = sheetMethods.nextRow();
    writeStringCell(row, 0, resources.getString("cavaets.ethnicity.text"), this.wrapStyle);
  }
  
  void writeReferenceHeader(Set<String> alleles) {
    Row header = sheetReferences.nextRow();
    int headerIdx = 0;
    for (String col : REFERENCE_COLUMNS) {
      writeHeaderCell(header, headerIdx, col);
      headerIdx += 1;
    }
    for (String allele : alleles) {
      writeHeaderCell(header, headerIdx, allele);
      headerIdx += 1;
    }
    sheetReferences.setColCount(headerIdx);
  }
  
  private static final int REFERENCE_POP_HEADER_COL_COUNT = 8;
  void writePopulation(String[] authors, Integer year, String pmid, String ethnicity, String population, String popInfo,
                       String subjectType, Integer subjectCount, String[] frequencies) {
    ethnicities.add(ethnicity);
    
    Row row = sheetReferences.nextRow();
    writeStringCell(row, 0, Optional.ofNullable(authors).map(a -> a[0]).orElse(""));
    if (year != null && year > 0) {
      writeIntegerCell(row, 1, year);
    }
    writeStringCell(row, 2, pmid);
    writeStringCell(row, 3, ethnicity);
    writeStringCell(row, 4, population);
    writeStringCell(row, 5, popInfo);
    writeStringCell(row, 6, subjectType);
    writeIntegerCell(row, 7, subjectCount);
    int i = REFERENCE_POP_HEADER_COL_COUNT;
    for (String frequency : frequencies) {
      if (frequency != null) {
        writeStringCell(row, i, frequency);
      }
      i += 1;
    }
  }
  
  private Row currentMinFreqRow;
  private Row currentFreqRow;
  private Row currentMaxFreqRow;
  private int currentPopSummaryAlleleCol;

  void writeEthnicityHeader(String ethnicity, int numAlleles) {
    Row row = sheetReferences.nextRow();
    writeTopBorderCell(row, 0, ethnicity);
    for (int i = 1; i < REFERENCE_POP_HEADER_COL_COUNT + numAlleles; i++) {
      writeTopBorderCell(row, i, "");
    }
    sheetReferences.sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, REFERENCE_POP_HEADER_COL_COUNT + numAlleles));
  }

  void startPopulationSummary() {
    currentFreqRow = sheetReferences.nextRow();
    currentMinFreqRow = sheetReferences.nextRow();
    currentMaxFreqRow = sheetReferences.nextRow();
    currentPopSummaryAlleleCol = REFERENCE_POP_HEADER_COL_COUNT;
    sheetReferences.nextRow();
    
    writeHighlightCell(currentMinFreqRow, REFERENCE_POP_HEADER_COL_COUNT-1, "Min");
    writeHighlightCell(currentFreqRow, REFERENCE_POP_HEADER_COL_COUNT-1, "Average");
    writeHighlightCell(currentMaxFreqRow, REFERENCE_POP_HEADER_COL_COUNT-1, "Max");
  }
  
  void writePopulationSummary(double minFreq, double freq, double maxFreq) {
    writeHighlightCell(currentMinFreqRow, currentPopSummaryAlleleCol, minFreq);
    writeHighlightCell(currentFreqRow, currentPopSummaryAlleleCol, freq);
    writeHighlightCell(currentMaxFreqRow, currentPopSummaryAlleleCol, maxFreq);
    currentPopSummaryAlleleCol += 1;
  }
  
  void writeEmptyPopulationSummary() {
    writeHighlightCell(currentMinFreqRow, currentPopSummaryAlleleCol, "");
    writeHighlightCell(currentFreqRow, currentPopSummaryAlleleCol, "");
    writeHighlightCell(currentMaxFreqRow, currentPopSummaryAlleleCol, "");
    currentPopSummaryAlleleCol += 1;
  }
  
  private static final String TITLE_TEMPLATE = "Frequencies of %s alleles in major race/ethnic groups";
  private static final String GENE_CELL_TEMPLATE = "%s allele";
  void writeEthnicity() {
    Row header = sheetAllele.nextRow();
    writeHeaderCell(header, 0, String.format(TITLE_TEMPLATE, geneSymbol));
    sheetAllele.sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, ethnicities.size()));

    header = sheetAllele.nextRow();
    writeHeaderCell(header, 0, String.format(GENE_CELL_TEMPLATE, geneSymbol));
    int col = 1;
    for (String ethnicity : ethnicities) {
      writeHeaderCell(header, col, ethnicity);
      col += 1;
    }
    sheetAllele.setColCount(col);
  }
  
  void writeEthnicitySummary(String allele, Double[] frequencies) {
    Row row = sheetAllele.nextRow();
    writeHeaderCell(row, 0, allele);

    for (int i = 0; i < frequencies.length; i++) {
      writeDoubleCell(row, 1+i, frequencies[i]);
    }
  }
  
  void writeNotesHeader() {
    Row row = sheetChanges.nextRow();
    writeHeaderCell(row, 0, "Date");
    writeHeaderCell(row, 1, "Note");
    sheetChanges.setColCount(2);
  }
  
  void writeNote(Date date, String note) {
    Row row = sheetChanges.nextRow();
    writeDateCell(row, 0, date);
    writeStringCell(row, 1, note, false);
  }

  @Override
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
}
