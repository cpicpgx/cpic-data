package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

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

  private static final String DEFAULT_SHEET_NAME = "Definitions"; 
  private static final String CELL_PATTERN_GENE = "Gene:%s";
  private static final String CELL_PATTERN_HEADER_ALLELE = "%s Allele";
  private static final String CELL_HEADER_FXN = "Allele Functional Status";
  private static final String FILE_NAME_PATTERN = "%s_allele_definition_table.xlsx";
  private static final Pattern CHR_PATTERN = Pattern.compile("NC_0+(\\d+)\\.\\d{2}");
  
  private String geneSymbol;
  private Sheet sheet;
  private Row nameRow;
  private Row proteinRow;
  private Row chromoRow;
  private Row geneRow;
  private Row dbsnpRow;
  
  private Map<Long, Integer> colLocationMap = new HashMap<>();

  /**
   * Constructor. Sets up the Apache POI objects needed to write the values to the file.
   * @param gene an HGNC gene symbol
   * @param modified the Date the allele data was last modified
   */
  AlleleDefinitionWorkbook(String gene, Date modified, String seqChr, String seqPro, String seqGen) {
    super();
    if (StringUtils.stripToNull(gene) == null) {
      throw new IllegalArgumentException("Gene must be specified");
    }
    
    this.geneSymbol = gene;

    this.sheet = getSheet(DEFAULT_SHEET_NAME);
    Row row = sheet.createRow(rowIdx++);

    writeStringCell(row, 0, String.format(CELL_PATTERN_GENE, this.geneSymbol));
    writeDateCell(row, modified);
    
    nameRow = sheet.createRow(rowIdx++);
    proteinRow = sheet.createRow(rowIdx++);
    chromoRow = sheet.createRow(rowIdx++);
    geneRow = sheet.createRow(rowIdx++);
    dbsnpRow = sheet.createRow(rowIdx++);
    
    Row headerRow = sheet.createRow(rowIdx);
    writeStringCell(headerRow, 0, String.format(CELL_PATTERN_HEADER_ALLELE, this.geneSymbol));
    writeStringCell(headerRow, 1, CELL_HEADER_FXN);
    
    Matcher m = CHR_PATTERN.matcher(seqChr);
    String chr = "";
    if (m.matches()) {
      chr = m.group(1);
      if (chr.equals("23")) {
        chr = "X";
      } else if (chr.equals("24")) {
        chr = "Y";
      }
    }
    
    writeStringCell(nameRow,  colIdx, "Nucleotide change per gene from http://www.pharmvar.org");
    writeStringCell(proteinRow, colIdx, String.format("Effect on protein (%s)", seqPro));
    writeStringCell(chromoRow, colIdx, String.format("Position at %s (Homo sapiens chromosome %s, GRCh38.p2", seqChr, chr));
    writeStringCell(geneRow, colIdx, String.format("Position at %s (%s RefSeqGene)", seqGen, gene));
    writeStringCell(dbsnpRow, colIdx, "rsID");
  }
  
  /**
   * Generates the file name with the gene symbol in it
   * @return a file name for this workbook
   */
  String getFilename() {
    return String.format(FILE_NAME_PATTERN, this.geneSymbol);
  }
  
  String getSheetName() {
    return DEFAULT_SHEET_NAME;
  }

  /**
   * Write an allele row. Do this before writing allele location values
   * @param name the name of the allele (e.g. "*2")
   * @param fxn the text of the functional status
   */
  void writeAllele(String name, String fxn) {
    Row row = sheet.createRow(++rowIdx);

    writeStringCell(row, 0, name);
    writeStringCell(row, 1, fxn);
  }

  /**
   * Write the value of the allele at the location represented by the given allele. This will write to the row of the 
   * last allele written by the last "writeAllele" method executed.
   * @param locId the ID of an allele that appears in this sheet
   * @param value the value to write to the allele-location intersecting cell
   */
  void writeAlleleLocationValue(Long locId, String value) {
    if (!colLocationMap.keySet().contains(locId)) {
      throw new IllegalArgumentException("No location with ID specified " + locId);
    }
    
    Row row = sheet.getRow(rowIdx);
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
    colIdx += 1;
    writeStringCell(nameRow, colIdx, name);
    writeStringCell(proteinRow, colIdx, protein);
    writeStringCell(chromoRow, colIdx, chromo);
    writeStringCell(geneRow, colIdx, gene);
    writeStringCell(dbsnpRow, colIdx, dbSnpId);
    
    colLocationMap.put(locId, colIdx);
  }
  
  void writeNote(String note) {
    if (note != null) {
      Row row = sheet.createRow(++rowIdx);
      writeStringCell(row, 0, StringUtils.strip(note), false);
    }
  }
}
