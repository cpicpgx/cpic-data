package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents an Excel workbook that holds allele definition information
 *
 * @author Ryan Whaley
 */
class AlleleDefinitionWorkbook extends AbstractWorkbook {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DEFAULT_SHEET_NAME = "Definitions";
  private static final String CELL_PATTERN_GENE = "Gene:%s";
  private static final String CELL_PATTERN_HEADER_ALLELE = "%s Allele";
  private static final String FILE_NAME_PATTERN = "%s-Allele_Definition_Table.xlsx";
  private static final Pattern CHR_PATTERN = Pattern.compile("NC_0+(\\d+)\\.\\d{2}");
  
  private String geneSymbol;
  private SheetWrapper sheet;
  private SheetWrapper historySheet;
  private Row nameRow;
  private Row proteinRow;
  private Row chromoRow;
  private Row geneRow;
  private Row dbsnpRow;
  private Row alleleRow;
  
  private Map<Long, Integer> colLocationMap = new HashMap<>();

  /**
   * Constructor. Sets up the Apache POI objects needed to write the values to the file.
   * @param gene an HGNC gene symbol
   */
  AlleleDefinitionWorkbook(String gene, String seqChr, String seqPro, String seqGen, String seqMrna, Long pvCount) {
    super();
    if (StringUtils.stripToNull(gene) == null) {
      throw new IllegalArgumentException("Gene must be specified");
    }
    
    this.geneSymbol = gene;

    this.sheet = findSheet(DEFAULT_SHEET_NAME);
    Row row = sheet.nextRow();

    writeBoldStringCell(row, 0, String.format(CELL_PATTERN_GENE, this.geneSymbol));
    
    nameRow = sheet.nextRow();
    proteinRow = sheet.nextRow();
    chromoRow = sheet.nextRow();
    geneRow = sheet.nextRow();
    dbsnpRow = sheet.nextRow();

    Row headerRow = sheet.nextRow();
    writeStringCell(headerRow, 0, String.format(CELL_PATTERN_HEADER_ALLELE, this.geneSymbol), leftTextStyle);

    String chr = "";
    if (StringUtils.isNotBlank(seqChr)) {
      Matcher m = CHR_PATTERN.matcher(seqChr);
      if (m.matches()) {
        chr = m.group(1);
        if (chr.equals("23")) {
          chr = "X";
        } else if (chr.equals("24")) {
          chr = "Y";
        }
      }
    } else {
      sf_logger.debug("No chr sequence ID");
    }

    if (pvCount > 0) {
      writeStringCell(nameRow, 0, "Nucleotide change per gene from http://www.pharmvar.org", leftTextStyle);
    } else if (StringUtils.isNotBlank(seqMrna)) {
      writeStringCell(nameRow, 0, String.format("Nucleotide change on cDNA (%s)", seqMrna), leftTextStyle);
    } else {
      writeStringCell(nameRow, 0, "Common name");
    }
    writeStringCell(proteinRow, 0, String.format("Effect on protein (%s)", seqPro), leftTextStyle);
    writeStringCell(chromoRow, 0, String.format("Position at %s (Homo sapiens chromosome %s, GRCh38.p2)", seqChr, chr), wrapStyle);
    writeStringCell(geneRow, 0, String.format("Position at %s (%s RefSeqGene)", seqGen, gene), wrapStyle);
    writeStringCell(dbsnpRow, 0, "rsID", leftTextStyle);
    
    this.sheet.setWidths(new Integer[]{40*256});
  }
  
  /**
   * Generates the file name with the gene symbol in it
   * @return a file name for this workbook
   */
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
  
  /**
   * Write an allele row. Do this before writing allele location values
   * @param name the name of the allele (e.g. "*2")
   */
  void writeAllele(String name) {
    alleleRow = sheet.nextRow();

    writeStringCell(alleleRow, 0, name, leftTextStyle);
  }

  /**
   * Write the value of the allele at the location represented by the given allele. This will write to the row of the 
   * last allele written by the last "writeAllele" method executed.
   * @param locId the ID of an allele that appears in this sheet
   * @param value the value to write to the allele-location intersecting cell
   */
  void writeAlleleLocationValue(Long locId, String value) {
    if (!colLocationMap.containsKey(locId)) {
      throw new IllegalArgumentException("No location with ID specified " + locId);
    }
    
    Row row = alleleRow;
    writeStringCell(row, colLocationMap.get(locId), value);
  }

  /**
   * Writes the column header information for a variant location
   * @param name the name of the allele
   * @param protein the name of this variant on the protein sequence
   * @param chromo the name of this variant on the chromosomal sequence
   * @param gene the name of this variant on the gene sequence
   * @param dbSnpId the ID of this variant assigned by dbSNP
   * @param locId the ID for this location assigned by the DB
   */
  void writeVariant(String name, String protein, String chromo, String gene, String dbSnpId, Long locId) {
    writeStringCell(nameRow, colIdx, name);
    writeStringCell(proteinRow, colIdx, protein);
    writeStringCell(chromoRow, colIdx, chromo);
    writeStringCell(geneRow, colIdx, gene);
    writeStringCell(dbsnpRow, colIdx, dbSnpId);
    
    colLocationMap.put(locId, colIdx);
    this.sheet.setColCount(colIdx+1);
    colIdx += 1;
  }
  
  void writeNotesHeader() {
    Row row = sheet.nextRow();
    writeBoldStringCell(row, 0, "NOTES:");
  }

  void writeNote(String note) {
    if (note != null) {
      Row row = sheet.nextRow();
      writeStringCell(row, 0, StringUtils.strip(note), false);
    }
  }
  
  private int nHistory = 0;
  void writeHistory(Date date, String note) {
    if (nHistory == 0) {
      historySheet = this.findSheet(HISTORY_SHEET_NAME);
      Row headerRow = historySheet.nextRow();
      writeHeaderCell(headerRow, 0, "Date");
      writeHeaderCell(headerRow, 1, "Note");
    }
    Row row = historySheet.nextRow();
    writeDateCell(row, 0, date);
    writeStringCell(row, 1, note,false);
    nHistory += 1;
  }
}
