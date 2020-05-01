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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses diplotype-phenotype translation files.
 *
 * This does not actually load any data into the DB, it checks the existing files to ensure the previously
 * manually-curated data matches the data inferred by the system.
 *
 * @author Ryan Whaley
 */
public class DiplotypePhenotypeImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern GENE_PATTERN = Pattern.compile("(\\w+)\\s*Diplotype");
  private static final int COL_IDX_DIP = 0;
  private static final String DIPLOTYPE_SEPARATOR = "/";
  private static final String[] sf_deleteStatements = new String[]{};
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
      if (headerText.toLowerCase().contains("phenotype")) {
        phenoIdx = i;
      } else if (headerText.toLowerCase().contains("activity")) {
        activityIdx = i;
      } else if (headerText.toLowerCase().contains("ehr priority")) {
        ehrIdx = i;
      }
    }

    if (phenoIdx < 0) {
      throw new NotFoundException("Couldn't find phenotype column");
    }
    if (activityIdx < 0) {
      throw new NotFoundException("Couldn't find activity column");
    }
    if (ehrIdx < 0) {
      throw new NotFoundException("Couldn't find EHR priority column");
    }

    Matcher m = GENE_PATTERN.matcher(geneText);
    if (!m.find()) {
      throw new NotFoundException("Couldn't find gene");
    }
    String geneSymbol = m.group(1);
    sf_logger.debug("loading gene {}", geneSymbol);

    int rowsProcessed = 0;
    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        // don't load rows that are footnotes (won't have a phenotype)
        if (row.hasNoText(phenoIdx)) {
          continue;
        }

        String dip = row.getNullableText(COL_IDX_DIP);
        String pheno = row.getNullableText(phenoIdx);
        Double activity = row.getNullableDouble(activityIdx);
        String ehr = row.getNullableText(ehrIdx);

        try {
          dbHarness.insert(dip, pheno, activity, ehr);
        } catch (PSQLException ex) {
          sf_logger.warn("found duplicate " + dip + " on row " + (i+1));
        }
        rowsProcessed += 1;
      }
      sf_logger.info("    {} rows processed", rowsProcessed);
      sf_logger.info("    {} diplotypes match failed", dbHarness.getFailCount());
    }
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
    private final Connection conn;
    private final String gene;
    private final PreparedStatement findDiplotype;
    private int failCount = 0;

    DbHarness(String gene) throws SQLException {
      this.gene = gene;
      this.conn = ConnectionFactory.newConnection();

      this.findDiplotype = this.conn.prepareStatement(
          "select p.phenotype, f.totalactivityscore, d.id, p.ehrpriority from gene_phenotype p " +
              "    join phenotype_function f on p.id = f.phenotypeId " +
              "    join phenotype_diplotype d on f.id = d.functionPhenotypeId where p.genesymbol=? and d.diplotype=?"
      );
    }

    void insert(String diplotype, String phenotype, Double activity, String ehr) throws SQLException {
      String phenoStripped = stripPhenotype(phenotype);

      findDiplotype.setString(1, gene);
      findDiplotype.setString(2, diplotype);
      try (ResultSet rs = findDiplotype.executeQuery()) {
        if (rs.next()) {
          String existingPhenotype = rs.getString(1);
          String existingActivityScore = rs.getString(2);
          String existingEhrPriority = rs.getString(4);

          if (!phenoStripped.equalsIgnoreCase(existingPhenotype)) {
            sf_logger.info("{} {}: P:{}<>{} AS:{}<>{}", gene, diplotype, phenoStripped, existingPhenotype, activity, existingActivityScore);
            failCount += 1;
          }
          if (!ehr.equalsIgnoreCase(existingEhrPriority) && !(ehr.equals("none") && existingEhrPriority == null)) {
            sf_logger.info("{} {}: EHR:{}<>{}", gene, diplotype, ehr, existingEhrPriority);
            failCount += 1;
          }

          if (rs.next()) {
            throw new RuntimeException("Unexpected second result for " + gene + " " + diplotype);
          }
        }
      }
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
      if (this.findDiplotype != null) {
        this.findDiplotype.close();
      }
      if (this.conn != null) {
        this.conn.close();
      }
    }

    public int getFailCount() {
      return failCount;
    }
  }
}
