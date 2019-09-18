package org.cpicpgx.importer;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse references for functional assignments from a directory of excel files.
 *
 * @author Ryan Whaley
 */
public class FunctionReferenceImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern sf_geneLabelPattern = Pattern.compile("GENE:\\s(\\w+)");
  private static Set<String> GENES_WITH_FINDINGS = new HashSet<>();
  static {
    GENES_WITH_FINDINGS.add("CACNA1S");
    GENES_WITH_FINDINGS.add("RYR1");
  }
  private static final int COL_IDX_ALLELE = 0;
  private static final int COL_IDX_FUNCTION = 1;
  private static final int COL_IDX_PMID = 3;
  private static final int COL_IDX_INVITRO = 4;
  private static final int COL_IDX_INVIVO = 5;
  
  private static final int COL_IDX_VARIANT_NAME = 2;
  private static final int COL_IDX_FINDING_FX = 4;
  private static final int COL_IDX_FINDING = 5;
  private static final int COL_IDX_PMIDS = 6;

  private static final String[] sf_deleteStatements = new String[]{
      "delete from function_reference"
  };
  private static final String DEFAULT_DIRECTORY = "allele_functionality_reference";

  public static void main(String[] args) {
    rebuild(new FunctionReferenceImporter(), args);
  }

  public FunctionReferenceImporter() { }
  
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.ALLELE_FUNCTION_REFERENCE;
  }

  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  void processWorkbook(WorkbookWrapper workbook) throws NotFoundException, SQLException {
    int rowIdx = 0;

    RowWrapper row = null;
    String geneSymbol = null;
    for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
      row = workbook.getRow(rowIdx);
      String geneLabel = row.getNullableText(0);
      if (geneLabel == null) continue;
      
      Matcher m = sf_geneLabelPattern.matcher(geneLabel);
      if (m.find()) {
        geneSymbol = m.group(1);
        break;
      }
    }
    
    if (geneSymbol == null) {
      throw new NotFoundException("Couldn't find gene symbol");
    }
    
    sf_logger.debug("This sheet is for {}, {}", geneSymbol, row.getNullableText(1));
    java.util.Date modDate = row.getNullableDate(1);
    
    rowIdx += 2; // move down 2 rows and start reading;
    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      dbHarness.updateModified(new java.sql.Date(modDate.getTime()));
      if (GENES_WITH_FINDINGS.contains(geneSymbol)) {
        processPerFinding(workbook, dbHarness, rowIdx);
      } else {
        processPerReference(workbook, dbHarness, rowIdx);
      }
    }
    addImportHistory(workbook.getFileName());
  }

  private void processPerReference(WorkbookWrapper workbook, DbHarness dbHarness, int rowIdx) throws SQLException {
    String currentAllele = null;
    String currentFunction = null;
    for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
      RowWrapper row = workbook.getRow(rowIdx);
      if (row.hasNoText(COL_IDX_PMID)) {
        continue;
      }

      if (row.getNullableText(COL_IDX_ALLELE) != null) {
        currentAllele = row.getNullableText(COL_IDX_ALLELE);
        currentFunction = row.getNullableText(COL_IDX_FUNCTION);
      }
      Long pmid = row.getNullableLong(COL_IDX_PMID);
      String inVitro = row.getNullableText(COL_IDX_INVITRO);
      String[] inVitroArray = inVitro == null ? null : inVitro.split(",\\s*");
      String inVivo = row.getNullableText(COL_IDX_INVIVO);
      String[] inVivoArray = inVivo == null ? null : inVivo.split(",\\s*");

      dbHarness.insert(currentAllele, currentFunction, pmid, inVitroArray, inVivoArray);
    }
  }
  
  private void processPerFinding(WorkbookWrapper workbook, DbHarness dbHarness, int rowIdx) throws SQLException {
    String currentAllele = null;
    String currentFunction = null;
    for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
      RowWrapper row = workbook.getRow(rowIdx);
      if (row.hasNoText(COL_IDX_FINDING)) {
        continue;
      }

      if (row.getNullableText(COL_IDX_VARIANT_NAME) != null) {
        currentAllele = row.getNullableText(COL_IDX_VARIANT_NAME);
        currentFunction = row.getNullableText(COL_IDX_FINDING_FX);
      }
      String finding = row.getNullableText(COL_IDX_FINDING);
      String[] pmids = row.getNullablePmids(COL_IDX_PMIDS);

      if (pmids != null) {
        for (String pmid : pmids) {
          dbHarness.insertFinding(currentAllele, currentFunction, pmid, finding);
        }
      }
    }
  }

  /**
   * Private class for handling DB interactions
   */
  static class DbHarness implements AutoCloseable {
    private Connection conn;
    private Map<String, Long> alleleNameMap = new HashMap<>();
    private PreparedStatement updateAlleleStmt;
    private PreparedStatement insertStmt;
    private PreparedStatement insertFinding;
    private String gene;
    
    DbHarness(String gene) throws SQLException {
      this.gene = gene;
      this.conn = ConnectionFactory.newConnection();

      try (PreparedStatement pstmt = this.conn.prepareStatement("select name, id from allele where allele.geneSymbol=?")) {
        pstmt.setString(1, gene);
        try (ResultSet rs = pstmt.executeQuery()) {
          while (rs.next()) {
            this.alleleNameMap.put(rs.getString(1), rs.getLong(2));
          }
        }
      }
      
      updateAlleleStmt = this.conn.prepareStatement("update allele set functionalstatus=? where id=?");
      insertStmt = this.conn.prepareStatement("insert into function_reference(alleleid, pmid, substrate_in_vitro, substrate_in_vivo) values (?, ?, ?, ?)");
      insertFinding = this.conn.prepareStatement("insert into function_reference(alleleid, pmid, finding) values (?, ?, ?)");
    }
    
    void updateModified(java.sql.Date date) throws SQLException {
      try (PreparedStatement stmt = this.conn.prepareStatement("update gene set functionalityreferencelastmodified=? where symbol=?")) {
        stmt.setDate(1, date);
        stmt.setString(2, gene);
        stmt.executeUpdate();
      }
    }
    
    void insert(String allele, String alleleFunction, Long pmid, String[] inVitro, String[] inVivo) throws SQLException {
      if (!this.alleleNameMap.containsKey(allele)) {
        sf_logger.warn("No allele defined with name {}", allele);
        return;
      }
      
      this.insertStmt.clearParameters();
      this.insertStmt.setLong(1, this.alleleNameMap.get(allele));
      this.insertStmt.setString(2, String.valueOf(pmid));
      
      Array inVitroArray = inVitro == null ? null : this.conn.createArrayOf("text", inVitro);
      if (inVitroArray == null) {
        this.insertStmt.setNull(3, Types.ARRAY);
      } else {
        this.insertStmt.setArray(3, inVitroArray);
      }
      
      Array inVivoArray = inVivo == null ? null : this.conn.createArrayOf("text", inVivo);
      if (inVivoArray == null) {
        this.insertStmt.setNull(4, Types.ARRAY);
      } else {
        this.insertStmt.setArray(4, inVivoArray);
      }
      
      this.insertStmt.executeUpdate();

      this.updateAlleleStmt.clearParameters();
      this.updateAlleleStmt.setString(1, normalizeFunction(alleleFunction));
      this.updateAlleleStmt.setLong(2, this.alleleNameMap.get(allele));
      this.updateAlleleStmt.executeUpdate();
    }

    void insertFinding(String allele, String alleleFunction, String pmid, String finding) throws SQLException {
      if (!this.alleleNameMap.containsKey(allele)) {
        sf_logger.warn("No allele defined with name {}", allele);
        return;
      }
      
      this.insertFinding.clearParameters();
      this.insertFinding.setLong(1, this.alleleNameMap.get(allele));
      this.insertFinding.setString(2, pmid);
      
      if (finding == null) {
        this.insertFinding.setNull(3, Types.VARCHAR);
      } else {
        this.insertFinding.setString(3, finding);
      }
      
      this.insertFinding.executeUpdate();

      this.updateAlleleStmt.clearParameters();
      this.updateAlleleStmt.setString(1, normalizeFunction(alleleFunction));
      this.updateAlleleStmt.setLong(2, this.alleleNameMap.get(allele));
      this.updateAlleleStmt.executeUpdate();
    }

    @Override
    public void close() throws SQLException {
      if (this.insertStmt != null) {
        this.insertStmt.close();
      }
      if (this.insertFinding != null) {
        this.insertFinding.close();
      }
      if (this.conn != null) {
        this.conn.close();
      }
    }
    
    private String normalizeFunction(String fn) {
      if (fn == null) {
        return null;
      }
      
      return fn
          .replaceAll("Function", "function")
          .replaceAll("unctione", "unction");
    }
  }
}
