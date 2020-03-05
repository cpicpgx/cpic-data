package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses diplotype-phenotype translation files
 *
 * @author Ryan Whaley
 */
public class DiplotypePhenotypeImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern GENE_PATTERN = Pattern.compile("(\\w+)\\s*Diplotype");
  private static final int COL_IDX_DIP = 0;
  private static final String DIPLOTYPE_SEPARATOR = "/";
  private static final String[] sf_deleteStatements = new String[]{
      "delete from phenotype_diplotype",
      "delete from gene_phenotype"
  };
  private static final String DEFAULT_DIRECTORY = "diplotype_phenotype_tables";

  public static void main(String[] args) {
    rebuild(new DiplotypePhenotypeImporter(), args);
  }
  
  public DiplotypePhenotypeImporter() { }
  
  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.DIPLOTYPE_PHENOTYPE;
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
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    int phenoIdx = -1;
    int ehrIdx = -1;
    int activityIdx = -1;

    // default diplo-pheno mappings should be on the first sheet
    workbook.switchToSheet(0);
    sf_logger.info("Reading sheet for phenotypes: {}", workbook.currentSheet.getSheetName());

    RowWrapper headerRow = workbook.getRow(0);
    String geneText = headerRow.getNullableText(0);
    if (geneText == null) {
      throw new NotFoundException("Couldn't find gene");
    }

    for (int i = 1; i <= headerRow.row.getLastCellNum(); i++) {
      if (headerRow.hasNoText(i)) continue;
      String headerText = headerRow.getNullableText(i);
      if (headerText.contains("Phenotype Summary")) {
        phenoIdx = i;
      } else if (headerText.contains("Activity")) {
        activityIdx = i;
      } else if (headerText.contains("EHR Priority")) {
        ehrIdx = i;
      }
    }
    
    Matcher m = GENE_PATTERN.matcher(geneText);
    if (!m.find()) {
      throw new NotFoundException("Couldn't find gene");
    }
    String geneSymbol = m.group(1);
    sf_logger.debug("loading gene {}", geneSymbol);

    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        // don't load rows that are footnotes (won't have a phenotype)
        if (row.hasNoText(phenoIdx)) {
          continue;
        }

        String dip = row.getNullableText(COL_IDX_DIP);
        String pheno = row.getNullableText(phenoIdx);
        Double activity = activityIdx >= 0 ? row.getNullableDouble(activityIdx) : null;
        String ehr = ehrIdx >= 0 ? row.getNullableText(ehrIdx) : null;

        try {
          dbHarness.insert(dip, pheno, activity, ehr);
        } catch (PSQLException ex) {
          sf_logger.warn("found duplicate " + dip + " on row " + (i+1));
        }
      }
    }

    // default CDS language should be on the second sheet
    workbook.switchToSheet(1);

    if (workbook.currentSheet == null) {
      sf_logger.warn("No CDS sheet for {}", geneSymbol);
      return;
    }

    sf_logger.info("Reading sheet for CDS: {}", workbook.currentSheet.getSheetName());
    
    int phenoCol = geneSymbol.equals("CYP2D6") ? 1 : 0;
    int textCol = geneSymbol.equals("CYP2D6") ? 3 : 2;

    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        String pheno = row.getNullableText(phenoCol);
        String text = row.getNullableText(textCol);
        
        if (pheno == null || text == null) continue;
        
        dbHarness.setConsultation(pheno, text);
      }
    }
    addImportHistory(workbook);
  }

  /**
   * Take a diplotype in the form *1/*2 and reverse it to be *2/*1. The "/" is considered teh delimiter.
   * @param dip a String diplotype like *1/*2
   * @return an equivalent reversed diplotype String
   */
  static String flipDip(String dip) {
    String[] alleles = dip.split(DIPLOTYPE_SEPARATOR);
    return alleles[1] + DIPLOTYPE_SEPARATOR + alleles[0];
  }

  /**
   * Test if the given diplotype is homozygous
   * @param dip a diplotype like *3/*3
   * @return true if the same allele appears on each side of the delimiter, false otherwise
   */
  static boolean isHom(String dip) {
    String[] alleles = dip.split(DIPLOTYPE_SEPARATOR);
    return Objects.equals(alleles[0], alleles[1]);
  }

  static class DbHarness implements AutoCloseable {
    private Connection conn;
    private String gene;
    private PreparedStatement insertStmt;
    private PreparedStatement insertDipStmt;
    private PreparedStatement updateTextStmt;
    private Map<String, Integer> phenoIdMap = new HashMap<>();

    DbHarness(String gene) throws SQLException {
      this.gene = gene;
      this.conn = ConnectionFactory.newConnection();

      insertStmt = this.conn.prepareStatement(
          "insert into gene_phenotype(geneSymbol, phenotype, activityscore, ehrPriority) values (?, ?, ?, ?) returning(id)"
      );
      insertDipStmt = this.conn.prepareStatement(
          "insert into phenotype_diplotype(phenotypeid, diplotype) values (?, ?)"
      );
      updateTextStmt = this.conn.prepareStatement(
          "update gene_phenotype set consultationtext=? where genesymbol=? and phenotype=?"
      );
    }

    void insert(String diplotype, String phenotype, Double activity, String ehr) throws SQLException {
      String phenoStripped = stripPhenotype(phenotype);
      Integer phenoId = this.phenoIdMap.get(phenoStripped);

      if (phenoId == null) {
        this.insertStmt.clearParameters();
        this.insertStmt.setString(1, this.gene);

        if (phenoStripped != null) {
          if (!phenotype.equals(gene + " " + phenoStripped) && !phenotype.equals(phenoStripped)) {
            sf_logger.warn("phenotype modified: {} >> {}", phenotype, phenoStripped);
          }
          this.insertStmt.setString(2, phenoStripped);
        } else {
          this.insertStmt.setNull(2, Types.VARCHAR);
        }

        if (activity != null) {
          this.insertStmt.setDouble(3, activity);
        } else {
          this.insertStmt.setNull(3, Types.NUMERIC);
        }

        if (ehr != null) {
          this.insertStmt.setString(4, ehr);
        } else {
          this.insertStmt.setNull(4, Types.VARCHAR);
        }

        try (ResultSet rs = this.insertStmt.executeQuery()) {
          rs.next();
          phenoId = rs.getInt(1);
          this.phenoIdMap.put(phenoStripped, phenoId);
        }
      }
      
      insertDipStmt.setInt(1, phenoId);
      insertDipStmt.setString(2, diplotype);
      insertDipStmt.executeUpdate();

      if (!isHom(diplotype)) {
        insertDipStmt.setString(2, flipDip(diplotype));
        insertDipStmt.executeUpdate();
      }
    }
    
    void setConsultation(String phenotype, String consultationText) throws SQLException {
      this.updateTextStmt.setString(1, consultationText);
      this.updateTextStmt.setString(2, this.gene);
      this.updateTextStmt.setString(3, stripPhenotype(phenotype));
      this.updateTextStmt.executeUpdate();
    }

    String stripPhenotype(String pheno) {
      if (pheno == null) {
        return null;
      }
      return StringUtils.stripToNull(
          pheno
              .replaceAll(this.gene, "")
              .replaceAll("\\s+", " ")
              .replaceAll("unction[abcdef]", "unction")
              .replaceAll("Function", "function")
              .replaceAll("[Mm]eta[zb]olizer[cd]*+", "metabolizer")
      );
    }

    @Override
    public void close() throws Exception {
      if (this.insertStmt != null) {
        this.insertStmt.close();
      }
      if (this.conn != null) {
        this.conn.close();
      }
    }
  }
}
