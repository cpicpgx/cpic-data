package org.cpicpgx.importer;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads in a given <code>.xlsx</code> file, parses it, and writes it to a postgres database.
 *
 * @author Ryan Whaley
 */
@SuppressWarnings("SpellCheckingInspection")
public class AlleleDefinitionImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int sf_variantColStart = 1;
  private static final Pattern sf_seqIdPattern = Pattern.compile("N\\D_\\d+\\.\\d+");
  private static final Pattern sf_rsidPattern = Pattern.compile("^rs\\d+$");
  private static final int sf_alleleRowStart = 7;

  private String m_gene;
  private int m_variantColEnd;
  private String m_proteinSeqId = "";
  private String m_chromoSeqId = "";
  private String m_geneSeqId = "";
  private String m_mrnaSeqId = "";
  private String[] m_legacyNames;
  private String[] m_proteinEffects;
  private String[] m_chromoPositions;
  private String[] m_genoPositions;
  private String[] m_dbSnpIds;
  private Map<String,Map<Integer,String>> m_alleles;
  private Map<String,String> m_alleleFunctionMap;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("i", true,"input file (xls)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      Path inputPath = Paths.get(cli.getOptionValue("i"));
      try (InputStream in = Files.newInputStream(inputPath)) {
        WorkbookWrapper workbook = new WorkbookWrapper(in);

        AlleleDefinitionImporter importer = new AlleleDefinitionImporter(workbook);
        importer.writeToDB();
        importer.writeNotes(workbook);
        importer.writeHistory(workbook);
      } catch (Exception ex) {
        throw new RuntimeException("Error processing file " + inputPath, ex);
      }
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }
  
  AlleleDefinitionImporter(WorkbookWrapper workbook) {
    readGene(workbook.currentSheet);
    readLegacyRow(workbook.currentSheet);
    readProteinRow(workbook.currentSheet);
    readChromoRow(workbook.currentSheet);
    readGenoRow(workbook.currentSheet);
    readDbSnpRow(workbook.currentSheet);
    readAlleles(workbook.currentSheet);
  }

  String getGene() {
    return m_gene;
  }
  
  private void readGene(Sheet sheet) {
    Row row = sheet.getRow(0);
    Optional<String> geneOpt = getCellValue(row, 0);
    m_gene = geneOpt
        .orElseThrow(IllegalStateException::new)
        .replaceAll("GENE:\\s*", "");
  }
  
  private void readLegacyRow(Sheet sheet) {
    Row row = sheet.getRow(1);
    m_variantColEnd = row.getLastCellNum();
    m_legacyNames = new String[m_variantColEnd];

    Cell description = row.getCell(0);
    findSeqId(description.getStringCellValue());
    
    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      m_legacyNames[i] = getCellValue(row, i).orElse(null);
    }
  }
  
  private void readProteinRow(Sheet sheet) {
    Row row = sheet.getRow(2);
    m_proteinEffects = new String[m_variantColEnd];
    
    Cell description = row.getCell(0);
    findSeqId(description.getStringCellValue());
    
    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      m_proteinEffects[i] = getCellValue(row, i).orElse(null);
    }
  }
  
  private void readChromoRow(Sheet sheet) {
    Row row = sheet.getRow(3);
    m_chromoPositions = new String[m_variantColEnd];

    Cell description = row.getCell(0);
    findSeqId(description.getStringCellValue());

    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      m_chromoPositions[i] = getCellValue(row, i).orElse(null);
    }
  }
  
  private void readGenoRow(Sheet sheet) {
    Row row = sheet.getRow(4);
    m_genoPositions = new String[m_variantColEnd];

    Cell description = row.getCell(0);
    findSeqId(description.getStringCellValue());

    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      m_genoPositions[i] = getCellValue(row, i).orElse(null);
    }
  }
  
  private void readDbSnpRow(Sheet sheet) {
    Row row = sheet.getRow(5);
    m_dbSnpIds = new String[m_variantColEnd];
    
    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      Optional<String> rsid = getCellValue(row, i);
      if (!rsid.isPresent()) {
        continue;
      }
      
      Matcher m = sf_rsidPattern.matcher(rsid.get());
      if (m.matches()) {
        m_dbSnpIds[i] = m.group();
      }
      else {
        sf_logger.warn("Invalid RSID found in {}, skipping", row.getCell(i).getAddress());
      }
    }
  }
  
  private void findSeqId(String cellContent) {
    if (StringUtils.isBlank(cellContent)) return;
    
    Matcher m = sf_seqIdPattern.matcher(cellContent);
    if (m.find()) {
      String seqId = m.group();
      if (seqId.startsWith("NG_")) {
        m_geneSeqId = seqId;
      } else if (seqId.startsWith("NM_")) {
        m_mrnaSeqId = seqId;
      } else if (seqId.startsWith("NC_")) {
        m_chromoSeqId = seqId;
      } else if (seqId.startsWith("NP_")) {
        m_proteinSeqId = seqId;
      }
    }
  }
  
  private void readAlleles(Sheet sheet) {
    m_alleles = new LinkedHashMap<>();
    m_alleleFunctionMap = new LinkedHashMap<>();
    
    for (int i=sf_alleleRowStart; i <= sheet.getLastRowNum(); i++) {
      try {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        
        Optional<String> alleleNameOpt = getCellValue(row, 0);
        if (!alleleNameOpt.isPresent()) {
          continue;
        }
        String alleleName = alleleNameOpt.get();
        if (alleleName.length() == 0) {
          break;
        }

        String alleleFunction = getCellValue(row, 1).orElse(null);
        m_alleleFunctionMap.put(alleleName, alleleFunction);

        Map<Integer, String> definition = new LinkedHashMap<>();
        for (int j = sf_variantColStart; j < m_variantColEnd; j++) {
          final int arrayIdx = j;
          getCellValue(row, j).ifPresent(s -> definition.put(arrayIdx, s));
        }

        m_alleles.put(alleleName, definition);
      } catch (Exception e) {
        sf_logger.error("Error parsing row {}", i+1);
        throw e;
      }
    }
  }

  private void writeNotes(WorkbookWrapper workbook) throws SQLException {
    List<String> notes = workbook.getNotes();

    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement noteInsert = conn.prepareStatement("insert into gene_note(geneSymbol, note, type, ordinal) values (?, ?, ?, ?)");
      int n = 0;
      for (String note : notes) {
        noteInsert.setString(1, m_gene);
        noteInsert.setString(2, note);
        noteInsert.setString(3, NoteType.ALLELE_DEFINITION.name());
        noteInsert.setInt(4, n);
        noteInsert.executeUpdate();
        n += 1;
      }
      sf_logger.info("created {} new notes", notes.size());
    }
  }

  void writeHistory(WorkbookWrapper workbook) throws SQLException {
    workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);

    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement insertStmt = conn.prepareStatement("insert into gene_note (geneSymbol, type, date, note, ordinal) values (?, ?, ?, ?, ?)");
      insertStmt.setString(1, m_gene);
      insertStmt.setString(2, NoteType.ALLELE_DEFINITION.name());
      
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        if (row.hasNoText(0)) continue;
        
        Date date = row.getNullableDate(0);
        String note = row.getNullableText(1);
        
        insertStmt.setDate(3, new java.sql.Date(date.getTime()));
        if (StringUtils.isNotBlank(note)) {
          insertStmt.setString(4, note);
        } else {
          insertStmt.setString(4, "n/a");
        }
        insertStmt.setLong(5, i);
        
        insertStmt.executeUpdate();
      }
    }
  }
  
  void writeToDB() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {

      PreparedStatement joinTableInsert = conn.prepareStatement("insert into allele_location_value(alleleid, locationid, variantallele) values (?,?,?)");
      
      PreparedStatement geneUpdate = conn.prepareStatement("update gene set genesequenceid=?,proteinsequenceid=?,chromosequenceid=?,mrnaSequenceId=? where symbol=?");
      geneUpdate.setString(1, m_geneSeqId);
      geneUpdate.setString(2, m_proteinSeqId);
      geneUpdate.setString(3, m_chromoSeqId);
      geneUpdate.setString(4, m_mrnaSeqId);
      geneUpdate.setString(5, m_gene);
      geneUpdate.executeUpdate();

      PreparedStatement seqLocInsert = conn.prepareStatement("insert into sequence_location(name, chromosomelocation, genelocation, proteinlocation, dbsnpid, geneSymbol) values (?,?,?,?,?,?) returning (id)");
      Integer[] locIdAssignements = new Integer[m_chromoPositions.length];
      int newLocations = 0;
      for (int i=0; i < m_chromoPositions.length; i++) {
        
        // here we want to guard against over-running the location columns
        // we can't rely on either the legacy row or the chromo row singly since there are sheets that have missing 
        // values in both so we need to check for either
        if (m_chromoPositions[i] == null && m_legacyNames[i] == null) {
          continue;
        }

        seqLocInsert.setString(1, m_legacyNames[i]);
        seqLocInsert.setString(2, m_chromoPositions[i]);
        seqLocInsert.setString(3, m_genoPositions[i]);
        seqLocInsert.setString(4, m_proteinEffects[i]);
        seqLocInsert.setString(5, m_dbSnpIds[i]);
        seqLocInsert.setString(6, m_gene);
        ResultSet rs = seqLocInsert.executeQuery();
        rs.next();
        int locId = rs.getInt(1);
        locIdAssignements[i] = locId;
        newLocations += 1;
      }
      sf_logger.info("created {} new locations", newLocations);

      PreparedStatement alleleInsert = conn.prepareStatement("insert into allele(geneSymbol, name, functionalstatus) values (?,?,?) returning (id)");
      for (String alleleName : m_alleles.keySet()) {
        alleleInsert.setString(1, m_gene);
        alleleInsert.setString(2, alleleName);
        alleleInsert.setString(3, m_alleleFunctionMap.get(alleleName));
        ResultSet rs = alleleInsert.executeQuery();
        rs.next();
        int alleleId = rs.getInt(1);
        
        Map<Integer,String> allelePosMap = m_alleles.get(alleleName);
        for (Integer locIdx : allelePosMap.keySet()) {
          joinTableInsert.setInt(1, alleleId);
          joinTableInsert.setInt(2, locIdAssignements[locIdx]);
          joinTableInsert.setString(3, allelePosMap.get(locIdx));
          joinTableInsert.executeUpdate();
        }
      }
      sf_logger.info("created {} new alleles", m_alleles.keySet().size());
    }
  }

  /**
   * Makes a string representation of a cell's value. Will log if the cell has extra whitespace preceding or succeeding 
   * the cell value.
   * @param cell a non-null Cell object 
   * @return an Optional String of the cell value
   */
  private static Optional<String> makeCellString(Cell cell) {
    String value = Objects.requireNonNull(cell).getStringCellValue();

    // if the cell doesn't exist or is 0-length, we don't care, don't log
    if (value == null || value.length() == 0) {
      return Optional.empty();
    }

    // strip down the cell value, including non-breaking spaces that Excel loves to introduce
    String strippedValue = trim(value);

    // return an empty value if there's nothing after strip
    if (strippedValue == null) return Optional.empty();
    
    // fix common misspellings
    strippedValue = strippedValue.replaceAll("Function", "function");
    
    return Optional.of(strippedValue);
  }

  /**
   * Trims ALL visible whitespace, including non-breaking spaces (<code>\h</code>)
   * @param value a String
   * @return a String without visible whitespace at the beginning or end of the string
   */
  static String trim(String value) {
    return StringUtils.stripToNull(value.replaceAll("(^\\h*)|(\\h*$)",""));
  }

  /**
   * Given a row and index, extract the cell's String value
   * @param row a non-null Row object
   * @param cellIndex the index of a cell in the given row
   * @return an Optional String of the cell value
   */
  private static Optional<String> getCellValue(Row row, int cellIndex) {
    Cell cell = Objects.requireNonNull(row).getCell(cellIndex);
    if (cell == null)  return Optional.empty();
    return makeCellString(cell);
  }
}
