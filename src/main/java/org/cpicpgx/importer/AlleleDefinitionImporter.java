package org.cpicpgx.importer;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class reads in a given <code>.xlsx</code> file, parses it, and writes it to a postgres database.
 *
 * @author Ryan Whaley
 */
public class AlleleDefinitionImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int sf_variantColStart = 2;
  private static final Pattern sf_seqIdPattern = Pattern.compile("N\\D_\\d+\\.\\d+");
  private static final Pattern sf_rsidPattern = Pattern.compile("^rs\\d+$");
  private static final int sf_alleleRowStart = 7;
  private static final String sf_dbUrl = "jdbc:postgresql://%s/cpic";
  private static final String sf_notes = "NOTES:";
  
  private Path m_inputPath;
  private String m_gene;
  private Date m_revisionDate;
  private int m_variantColEnd;
  private String m_proteinSeqId;
  private String m_chromoSeqId;
  private String m_geneSeqId;
  private String[] m_legacyNames;
  private String[] m_proteinEffects;
  private String[] m_chromoPositions;
  private String[] m_genoPositions;
  private String[] m_dbSnpIds;
  private Map<String,Map<Integer,String>> m_alleles;
  private Map<String,String> m_alleleFunctionMap;
  private int m_notesRowStart = -1;
  private List<String> m_notes;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("i", true,"input file (xls)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);
      
      AlleleDefinitionImporter importer = new AlleleDefinitionImporter(cli.getOptionValue("i"));
      importer.writeToDB();
    } catch (ParseException|SQLException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }
  
  AlleleDefinitionImporter(String filePath) {
    if (filePath == null) {
      throw new IllegalArgumentException("No file specified");
    }
    
    m_inputPath = Paths.get(filePath);
    if (!m_inputPath.toFile().exists()) {
      throw new IllegalArgumentException("File does not exist: " + filePath);
    }
    if (!m_inputPath.toFile().isFile()) {
      throw new IllegalArgumentException("Input is not a file: " + filePath);
    }
    readData();
  }
  
  private void readData() {
    sf_logger.info("Parsing {}", m_inputPath);
    
    try (InputStream in = Files.newInputStream(m_inputPath)) {
      Workbook workbook = WorkbookFactory.create(in);
      
      Sheet sheet = workbook.getSheetAt(0);

      readGene(sheet);
      
      readRevision(sheet);
      
      readLegacyRow(sheet);
      readProteinRow(sheet);
      readChromoRow(sheet);
      readGenoRow(sheet);
      readDbSnpRow(sheet);
      readAlleles(sheet);
      readNotes(sheet);
      
      workbook.close();
    } catch (IOException|InvalidFormatException e) {
      sf_logger.error("Couldn't process sheet", e);
    }
    
    sf_logger.info("parsed worksheet successfully, last modified {}", m_revisionDate);
  }
  
  private void readGene(Sheet sheet) {
    Row row = sheet.getRow(0);
    m_gene = trim(row.getCell(0).getStringCellValue().replaceAll("GENE:", ""));
  }
  
  private void readRevision(Sheet sheet) {
    Row row = sheet.getRow(0);
    m_revisionDate = row.getCell(1).getDateCellValue();
  }
  
  private void readLegacyRow(Sheet sheet) {
    Row row = sheet.getRow(1);
    m_variantColEnd = row.getLastCellNum();
    m_legacyNames = new String[m_variantColEnd];
    
    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      Cell cell = row.getCell(i);
      m_legacyNames[i] = trim(cell.getStringCellValue());
    }
  }
  
  private void readProteinRow(Sheet sheet) {
    Row row = sheet.getRow(2);
    m_proteinEffects = new String[m_variantColEnd];
    
    Cell description = row.getCell(1);
    m_proteinSeqId = findSeqId(description.getStringCellValue()).orElse("");
    
    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      Cell cell = row.getCell(i);
      m_proteinEffects[i] = trim(cell.getStringCellValue());
    }
  }
  
  private void readChromoRow(Sheet sheet) {
    Row row = sheet.getRow(3);
    m_chromoPositions = new String[m_variantColEnd];

    Cell description = row.getCell(1);
    m_chromoSeqId = findSeqId(description.getStringCellValue()).orElse("");

    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      Cell cell = row.getCell(i);
      m_chromoPositions[i] = trim(cell.getStringCellValue());
    }
  }
  
  private void readGenoRow(Sheet sheet) {
    Row row = sheet.getRow(4);
    m_genoPositions = new String[m_variantColEnd];

    Cell description = row.getCell(1);
    m_geneSeqId = findSeqId(description.getStringCellValue()).orElse("");

    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      Cell cell = row.getCell(i);
      m_genoPositions[i] = trim(cell.getStringCellValue());
    }
  }
  
  private void readDbSnpRow(Sheet sheet) {
    Row row = sheet.getRow(5);
    m_dbSnpIds = new String[m_variantColEnd];
    
    for (int i=sf_variantColStart; i < m_variantColEnd; i++) {
      Cell cell = row.getCell(i);
      if (cell == null) {
        continue;
      }
      String rsid = trim(cell.getStringCellValue());
      if (rsid == null) {
        continue;
      }
      
      Matcher m = sf_rsidPattern.matcher(rsid);
      if (m.matches()) {
        m_dbSnpIds[i] = rsid;
      }
      else {
        sf_logger.warn("Invalid RSID found in {}, skipping", cell.getAddress().toString());
      }
    }
  }
  
  private Optional<String> findSeqId(String cellContent) {
    Matcher m = sf_seqIdPattern.matcher(cellContent);
    if (m.find()) {
      return Optional.of(m.group());
    }
    return Optional.empty();
  }
  
  private void readAlleles(Sheet sheet) {
    m_alleles = new LinkedHashMap<>();
    m_alleleFunctionMap = new LinkedHashMap<>();
    
    for (int i=sf_alleleRowStart; i < sheet.getLastRowNum(); i++) {
      try {
        Row row = sheet.getRow(i);
        if (row == null || row.getCell(0) == null) {
          m_notesRowStart = i;
          break;
        }
        String alleleName = row.getCell(0).getStringCellValue();
        if (alleleName.length() == 0 || alleleName.contains(sf_notes)) {
          m_notesRowStart = i;
          break;
        }

        String alleleFunction = row.getCell(1).toString();
        m_alleleFunctionMap.put(trim(alleleName), trim(alleleFunction));

        Map<Integer, String> definition = new LinkedHashMap<>();
        for (int j = sf_variantColStart; j < m_variantColEnd; j++) {
          Cell cell = row.getCell(j);
          if (cell != null && cell.getStringCellValue() != null && cell.getStringCellValue().length() > 0) {
            definition.put(j, trim(cell.getStringCellValue()));
          }
        }

        m_alleles.put(trim(alleleName), definition);
      } catch (Exception e) {
        sf_logger.error("Error parsing row {}", i+1);
        throw e;
      }
    }
  }
  
  private void readNotes(Sheet sheet) {
    m_notes = new ArrayList<>();
    for (int i = m_notesRowStart+1; m_notesRowStart > 0 && i < sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) {
        continue;
      }
      Cell cell = row.getCell(0);
      if (cell == null || cell.getStringCellValue().length() == 0 || cell.getStringCellValue().contains(sf_notes)) {
        continue;
      }
      m_notes.add(row.getCell(0).toString());
    }
  }
  
  void writeToDB() throws SQLException {

    ResourceBundle resources = ResourceBundle.getBundle("cpicData");
    
    try (Connection conn = DriverManager.getConnection(
        String.format(sf_dbUrl, resources.getString("db.host")), 
        resources.getString("db.user"), 
        resources.getString("db.pass"))
    ) {
      
      String[] statements = new String[]{
          "delete from translation_note where hgncid=?",
          "delete from allele_location_value where alleleid in (select id from allele where hgncid=?)",
          "delete from allele where hgncid=?",
          "delete from sequence_location where hgncid=?"
      };
      for (String statement : statements) {
        PreparedStatement notesDelete = conn.prepareStatement(statement);
        notesDelete.setString(1, m_gene);
        notesDelete.executeUpdate();
      }
      
      PreparedStatement joinTableInsert = conn.prepareStatement("insert into allele_location_value(alleleid, locationid, variantallele) values (?,?,?)");
      
      PreparedStatement geneUpdate = conn.prepareStatement("update gene set genesequenceid=?,proteinsequenceid=?,chromosequenceid=? where hgncid=?");
      geneUpdate.setString(1, m_geneSeqId);
      geneUpdate.setString(2, m_proteinSeqId);
      geneUpdate.setString(3, m_chromoSeqId);
      geneUpdate.setString(4, m_gene);
      geneUpdate.executeUpdate();

      PreparedStatement seqLocInsert = conn.prepareStatement("insert into sequence_location(name, chromosomelocation, genelocation, proteinlocation, dbsnpid, hgncid) values (?,?,?,?,?,?) returning (id)");
      Integer[] locIdAssignements = new Integer[m_chromoPositions.length];
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
        Integer locId = rs.getInt(1);
        locIdAssignements[i] = locId;
      }
      sf_logger.info("created {} new locations", m_chromoPositions.length);

      PreparedStatement alleleInsert = conn.prepareStatement("insert into allele(hgncId, name, functionalstatus) values (?,?,?) returning (id)");
      for (String alleleName : m_alleles.keySet()) {
        alleleInsert.setString(1, m_gene);
        alleleInsert.setString(2, alleleName);
        alleleInsert.setString(3, m_alleleFunctionMap.get(alleleName));
        ResultSet rs = alleleInsert.executeQuery();
        rs.next();
        Integer alleleId = rs.getInt(1);
        
        Map<Integer,String> allelePosMap = m_alleles.get(alleleName);
        for (Integer locIdx : allelePosMap.keySet()) {
          joinTableInsert.setInt(1, alleleId);
          joinTableInsert.setInt(2, locIdAssignements[locIdx]);
          joinTableInsert.setString(3, allelePosMap.get(locIdx));
          joinTableInsert.executeUpdate();
        }
      }
      sf_logger.info("created {} new alleles", m_alleles.keySet().size());

      PreparedStatement noteInsert = conn.prepareStatement("insert into translation_note(hgncId, note) values (?,?)");
      for (String note : m_notes) {
        noteInsert.setString(1, m_gene);
        noteInsert.setString(2, note);
        noteInsert.executeUpdate();
      }
      sf_logger.info("created {} new notes", m_notes.size());
    }
  }
  
  private static String trim(String value) {
    if (value == null) {
      return null;
    }
    return StringUtils.stripToNull(value.replaceAll("(^\\h*)|(\\h*$)",""));
  }
}
