package org.cpicpgx.workbook;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.cpicpgx.db.LookupMethod;

import java.util.*;

/**
 * This is a sheet that displays allele frequencies in different aggregate views in different tabs
 *
 * @author Ryan Whaley
 */
public class FrequencyWorkbook extends AbstractWorkbook {
  private static final String FILE_NAME_PATTERN = "%s_frequency_table.xlsx";

  private static final String SHEET_NAME_METHODS = "Methods and Caveats";
  private static final String SHEET_NAME_REFERENCES = "References";
  private static final String SHEET_NAME_ALLELE = "Allele frequency";
  private static final String SHEET_DIPLOTYPE = "Diplotype frequency";
  private static final String SHEET_PHENOTYPE = "Phenotype frequency";

  private static final String[] REFERENCE_COLUMNS = new String[]{
      "Authors", "Year", "PMID", "Population group", "Population", "Add'l population info", "Subject type", "N subjects genotyped"
  };

  private final String geneSymbol;
  private final boolean byActivityScore;
  private SheetWrapper sheetReferences;
  private SheetWrapper sheetAllele;
  private SheetWrapper sheetDiplotype;
  private SheetWrapper sheetPhenotype;

  public FrequencyWorkbook(String geneSymbol, LookupMethod lookupMethod) {
    super();

    if (StringUtils.isBlank(geneSymbol)) {
      throw new IllegalArgumentException("Must supply a gene");
    }
    this.geneSymbol = geneSymbol;
    this.byActivityScore = lookupMethod == LookupMethod.ACTIVITY_SCORE;
  }

  public void writeMethods(String methods) {
    SheetWrapper sheetMethods = findSheet(SHEET_NAME_METHODS);
    sheetMethods.setColCount(1);
    sheetMethods.setWidths(new Integer[]{100*256});

    for (String line : methods.split("\n")) {
      Row row = sheetMethods.nextRow();
      writeStringCell(row, 0, line, wrapStyle);
    }
  }

  public void writeReferenceHeader(Set<String> alleles) {
    sheetReferences = findSheet(SHEET_NAME_REFERENCES);
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
  public void writePopulation(String[] authors, Integer year, String pmid, String ethnicity, String population,
                              String popInfo, String subjectType, Integer subjectCount, String[] frequencies) {
    writePopulation(authors, year, pmid, ethnicity, population, popInfo, subjectType, subjectCount,
        Arrays.asList(frequencies));
  }
  public void writePopulation(String[] authors, Integer year, String pmid, String ethnicity, String population,
                              String popInfo, String subjectType, Integer subjectCount, List<String> frequencies) {
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
    if (subjectCount == null) {
      writeStringCell(row, 7, "");
    } else {
      writeIntegerCell(row, 7, subjectCount);
    }
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

  public void writeEthnicityHeader(String ethnicity, int numAlleles) {
    Row row = sheetReferences.nextRow();
    writeTopBorderCell(row, 0, ethnicity);
    for (int i = 1; i < REFERENCE_POP_HEADER_COL_COUNT + numAlleles; i++) {
      writeTopBorderCell(row, i, "");
    }
    sheetReferences.sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, REFERENCE_POP_HEADER_COL_COUNT + numAlleles));
  }

  public void startPopulationSummary() {
    currentFreqRow = sheetReferences.nextRow();
    currentMinFreqRow = sheetReferences.nextRow();
    currentMaxFreqRow = sheetReferences.nextRow();
    currentPopSummaryAlleleCol = REFERENCE_POP_HEADER_COL_COUNT;
    sheetReferences.nextRow();
    
    writeHighlightCell(currentMinFreqRow, REFERENCE_POP_HEADER_COL_COUNT-1, "Min");
    writeHighlightCell(currentFreqRow, REFERENCE_POP_HEADER_COL_COUNT-1, "Average");
    writeHighlightCell(currentMaxFreqRow, REFERENCE_POP_HEADER_COL_COUNT-1, "Max");
  }

  public void writePopulationSummary(double minFreq, double freq, double maxFreq) {
    writeHighlightCell(currentMinFreqRow, currentPopSummaryAlleleCol, minFreq);
    writeHighlightCell(currentFreqRow, currentPopSummaryAlleleCol, freq);
    writeHighlightCell(currentMaxFreqRow, currentPopSummaryAlleleCol, maxFreq);
    currentPopSummaryAlleleCol += 1;
  }

  public void writeEmptyPopulationSummary() {
    writeHighlightCell(currentMinFreqRow, currentPopSummaryAlleleCol, "");
    writeHighlightCell(currentFreqRow, currentPopSummaryAlleleCol, "");
    writeHighlightCell(currentMaxFreqRow, currentPopSummaryAlleleCol, "");
    currentPopSummaryAlleleCol += 1;
  }

  public void writeReferencePopulationSummary(Double freq) {
    writeHighlightCell(currentFreqRow, currentPopSummaryAlleleCol, freq);
    writeHighlightCell(currentMinFreqRow, currentPopSummaryAlleleCol, "");
    writeHighlightCell(currentMaxFreqRow, currentPopSummaryAlleleCol, "");
    currentPopSummaryAlleleCol += 1;
  }

  private static final String TITLE_TEMPLATE = "Frequencies of %s alleles in biogeographical groups";
  private static final String GENE_CELL_TEMPLATE = "%s allele";

  /**
   * Start the allele frequency file
   */
  public void writeAlleleFrequencyHeader(List<String> bioGeoGroups) {
    sheetAllele = findSheet(SHEET_NAME_ALLELE);
    Row header = sheetAllele.nextRow();
    writeHeaderCell(header, 0, String.format(TITLE_TEMPLATE, geneSymbol));
    sheetAllele.sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, bioGeoGroups.size()));

    header = sheetAllele.nextRow();
    writeHeaderCell(header, 0, String.format(GENE_CELL_TEMPLATE, geneSymbol));
    int col = 1;
    for (String group : bioGeoGroups) {
      writeHeaderCell(header, col, group);
      col += 1;
    }
    sheetAllele.setColCount(col);
  }

  /**
   * Write specific allele frequency data
   * @param allele the gene allele
   * @param frequencies an array of frequency data
   */
  public void writeAlleleFrequency(String allele, Double[] frequencies) {
    Row row = sheetAllele.nextRow();
    writeStringCell(row, 0, allele, false);

    for (int i = 0; i < frequencies.length; i++) {
      writeDoubleCell(row, 1+i, frequencies[i]);
    }
  }

  private static final String TITLE_DIPLO_TEMPLATE = "Frequencies of %s diplotypes in biogeographical groups";
  /**
   * Write header for the diplotype sheet
   */
  public void writeDiplotypeFrequencyHeader(List<String> bioGeoGroups) {
    sheetDiplotype = findSheet(SHEET_DIPLOTYPE);
    Row header = sheetDiplotype.nextRow();
    writeHeaderCell(header, 0, String.format(TITLE_DIPLO_TEMPLATE, geneSymbol));
    sheetDiplotype.sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, bioGeoGroups.size()));

    header = sheetDiplotype.nextRow();
    writeHeaderCell(header, 0, String.format(GENE_CELL_TEMPLATE, geneSymbol));
    int col = 1;
    for (String group : bioGeoGroups) {
      writeHeaderCell(header, col, group);
      col += 1;
    }
    sheetDiplotype.setColCount(col);
  }

  public void writeDiplotypeFrequency(String diplotype, Double[] frequencies) {
    Row row = sheetDiplotype.nextRow();
    writeStringCell(row, 0, diplotype, false);

    for (int i = 0; i < frequencies.length; i++) {
      writeDoubleCell(row, 1+i, frequencies[i]);
    }
  }

  private static final String TITLE_PHENO_TEMPLATE = "Frequencies of %s phenotypes in biogeographical groups";
  /**
   * Write header for the phenotype sheet
   */
  public void writePhenotypeFrequencyHeader(List<String> bioGeoGroups) {
    sheetPhenotype = findSheet(SHEET_PHENOTYPE);
    Row header = sheetPhenotype.nextRow();
    writeHeaderCell(header, 0, String.format(TITLE_PHENO_TEMPLATE, geneSymbol));
    sheetPhenotype.sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, bioGeoGroups.size()));

    header = sheetPhenotype.nextRow();
    writeHeaderCell(header, 0, byActivityScore ? "Activity Score" : "Phenotype");
    int col = 1;
    for (String group : bioGeoGroups) {
      writeHeaderCell(header, col, group);
      col += 1;
    }
    sheetPhenotype.setColCount(col);
  }

  public void writePhenotypeFrequency(String phenoOrScore, Double[] frequencies) {
    Row row = sheetPhenotype.nextRow();
    writeStringCell(row, 0, phenoOrScore, false);

    for (int i = 0; i < frequencies.length; i++) {
      writeDoubleCell(row, 1+i, frequencies[i]);
    }
  }

  @Override
  public String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
}
