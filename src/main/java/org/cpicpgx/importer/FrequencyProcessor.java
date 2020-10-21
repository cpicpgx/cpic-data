package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that processes information from a Sheet rows into data model instances.
 * 
 * This should be instantiated in a <code>try</code> clause since it's {@link AutoCloseable} for the wrapped database 
 * connection.
 *
 * @author Ryan Whaley
 */
public class FrequencyProcessor extends DbHarness {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private final Map<Integer, Long> colIdxAlleleIdMap = new HashMap<>();
  private final PreparedStatement insertStatement;
  private final PreparedStatement insertPopulation;
  private final PreparedStatement insertHistory;
  private final PreparedStatement updateMethods;
  private final PreparedStatement updateDiplotypeFrequency;
  private final PreparedStatement updateAlleleFrequency;
  private final PreparedStatement updateResultFrequency;
  private final PublicationCatalog publicationCatalog;
  private final Map<String, Long> alleleNameMap;
  private final LookupMethod lookupMethod;

  private int colStartOffset = 0;

  /**
   * Construct the FrequencyProcessor
   * @param gene the Gene symbol for the gene this frequency data is for
   * @param headerRow the header Row from the frequency data sheet
   * @throws SQLException can occur when reading the header row
   */
  FrequencyProcessor(String gene, RowWrapper headerRow) throws SQLException, NotFoundException {
    super(FileType.FREQUENCY);
    publicationCatalog = new PublicationCatalog(getConnection());

    alleleNameMap = new HashMap<>();
    //language=PostgreSQL
    PreparedStatement pstmt = prepare("select name, id from allele where allele.geneSymbol=?");
    if (gene.equals("HLA")) {
      String[] hlaGenes = new String[]{"HLA-A", "HLA-B"};
      for (String hlaGene : hlaGenes) {
        pstmt.setString(1, hlaGene);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
          alleleNameMap.put(hlaGene + rs.getString(1), rs.getLong(2));
        }
      }
    } 
    else {
      pstmt.setString(1, gene);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        alleleNameMap.put(rs.getString(1), rs.getLong(2));
      }
    }

    //language=PostgreSQL
    PreparedStatement luStmt = prepare("select lookupmethod from gene where symbol=?");
    luStmt.setString(1, gene);
    try (ResultSet luRs = luStmt.executeQuery()) {
      if (luRs.next()) {
        lookupMethod = LookupMethod.valueOf(luRs.getString(1));
      } else {
        throw new RuntimeException("No Gene lookup method found for " + gene);
      }
    }

    //language=PostgreSQL
    this.insertStatement =
        prepare("insert into allele_frequency(alleleid, population, frequency, label) values (?, ?, ?, ?)");

    //language=PostgreSQL
    this.insertPopulation =
        prepare("insert into population(ethnicity, population, populationinfo, subjecttype, subjectcount, publicationId) values (?, ?, ?, ?, ?, ?) returning (id)");

    //language=PostgreSQL
    this.insertHistory =
        prepare("insert into change_log(entityId, note, type, date) values (?, ?, ?, ?)");
    this.insertHistory.setString(1, gene);
    this.insertHistory.setString(3, FileType.FREQUENCY.name());

    //language=PostgreSQL
    this.updateMethods =
        prepare("update gene set frequencyMethods=? where symbol=?");
    this.updateMethods.setString(2, gene);

    //language=PostgreSQL
    this.updateDiplotypeFrequency = prepare("update gene_result_diplotype d set frequency=?::jsonb where diplotypekey=?::jsonb and functionphenotypeid in (select l.id from gene_result_lookup l join gene_result r on (l.phenotypeId=r.id) where r.genesymbol=?)");
    this.updateDiplotypeFrequency.setString(3, gene);

    //language=PostgreSQL
    this.updateAlleleFrequency = prepare("update allele d set frequency=?::jsonb where name=? and genesymbol=?");
    this.updateAlleleFrequency.setString(3, gene);

    //language=PostgreSQL
    if (lookupMethod == LookupMethod.ACTIVITY_SCORE) {
      this.updateResultFrequency = prepare("update gene_result d set frequency=?::jsonb where activityscore=? and genesymbol=?");
    } else {
      this.updateResultFrequency = prepare("update gene_result d set frequency=?::jsonb where result=? and genesymbol=?");
    }
    this.updateResultFrequency.setString(3, gene);

    for (short i = headerRow.row.getFirstCellNum(); i < headerRow.row.getLastCellNum(); i++) {
      String cellText = headerRow.getNullableText(i);
      if (StringUtils.isNotBlank(cellText) && cellText.contains("Authors")) {
        colStartOffset = i;
        sf_logger.debug("Will offset authors at column {}", i);
        sf_logger.debug("Will get pmid at column {}", getPmidIdx());
        sf_logger.debug("Will get N at column {}", getNIdx());
      }
      if (StringUtils.isNotBlank(cellText) && alleleNameMap.containsKey(cellText)) {
        colIdxAlleleIdMap.put((int)i, alleleNameMap.get(cellText));
        sf_logger.debug("Will get {} frequencies from column {}", cellText, i);
      }
    }
    
    if (colIdxAlleleIdMap.size() == 0) {
      throw new NotFoundException("No allele columns could be found for alleles " + String.join("; ", alleleNameMap.keySet()));
    }

    // clear unused population
    int delCount = 0;
    //language=PostgreSQL
    delCount += prepare("delete from population where id not in (select population from allele_frequency)").executeUpdate();
    sf_logger.debug("cleared {} unused population records", delCount);
  }

  /**
   * Read frequency data from the given row at the given column index and writes it to the DB
   * @param row a row of frequency data
   * @param alleleColIdx the index of the allele column to read
   * @param populationId the ID of the population DB record this frequency is for
   * @throws SQLException can occur when writing to the DB
   */
  private void insertFrequency(RowWrapper row, Integer alleleColIdx, Long populationId) throws SQLException {
    if (alleleColIdx == null || !colIdxAlleleIdMap.containsKey(alleleColIdx)) {
      throw new RuntimeException("Allele column index invalid: " + alleleColIdx);
    }
    
    Long alleleId = colIdxAlleleIdMap.get(alleleColIdx);
    if (alleleId == null) {
      throw new RuntimeException("No allele ID found for " + alleleColIdx);
    }
    
    String label = row.getNullableText(alleleColIdx);
    Double value = row.getNullableDouble(alleleColIdx);
    
    this.insertStatement.setLong(1, alleleId);
    this.insertStatement.setLong(2, populationId);
    if (value != null) {
      // some older frequency files use the 0-100 percentage range instead of 0-1 decimal range, convert if found
      if (value > 1) {
        value = value / 100;
      }
      this.insertStatement.setDouble(3, value);
    } else {
      this.insertStatement.setNull(3, Types.DOUBLE);
    }
    if (label != null) {
      this.insertStatement.setString(4, label);
    } else {
      this.insertStatement.setNull(4, Types.VARCHAR);
    }
    int result = this.insertStatement.executeUpdate();
    if (result == 0) {
      throw new RuntimeException("Insert resulted in no rows");
    }
    this.insertStatement.clearParameters();
  }

  /**
   * Read the given population row and write contents to the DB
   * @param row a population Row
   * @throws SQLException can occur when writing data to the DB
   */
  void insertPopulation(RowWrapper row) throws SQLException {
    if (row.hasNoText(4)) return;

    String author = row.getNullableText(getAuthorIdx());
    String pubYear = row.getNullableText(getPubYearIdx(), true);
    String externalId = row.getNullableText(getPmidIdx(), true);
    Integer publicationId = this.publicationCatalog.lookupId(externalId, pubYear, author);

    this.insertPopulation.setString(1, row.getNullableText(getEthIdx()));
    this.insertPopulation.setString(2, row.getNullableText(getPopIdx()));
    this.insertPopulation.setString(3, row.getNullableText(getPopInfoIdx()));
    this.insertPopulation.setString(4, row.getNullableText(getSubjTypeIdx()));
    Long nSubjects = 0L;
    try {
      nSubjects = row.getNullableLong(getNIdx());
    } catch (NumberFormatException ex) {
      sf_logger.warn(ex.getMessage());
    }
    this.insertPopulation.setLong(5, nSubjects);
    if (publicationId != null) {
      this.insertPopulation.setInt(6, publicationId);
    } else {
      this.insertPopulation.setNull(6, Types.INTEGER);
    }
    ResultSet rs = this.insertPopulation.executeQuery();
    if (!rs.next()) {
      throw new RuntimeException("Insert failed");
    }
    Long popId = rs.getLong(1);
    
    for (Integer colIdx : colIdxAlleleIdMap.keySet()) {
      insertFrequency(row, colIdx, popId);
    }
    this.insertPopulation.clearParameters();
  }

  void insertHistory(java.util.Date date, String note) throws SQLException {
    if (note.equalsIgnoreCase(AbstractWorkbook.LOG_FILE_CREATED)) return;

    if (StringUtils.isNotBlank(note)) {
      insertHistory.setString(2, note);
    } else {
      insertHistory.setString(2, Constants.NA);
    }
    insertHistory.setDate(4, new Date(date.getTime()));
    insertHistory.executeUpdate();
  }

  void updateMethods(String methodsText) throws SQLException {
    if (!StringUtils.isBlank(methodsText)) {
      this.updateMethods.setString(1, methodsText);
      this.updateMethods.executeUpdate();
    }
  }

  void updateDiplotypeFrequency(JsonObject diplotype, JsonObject frequency) throws Exception {


    this.updateDiplotypeFrequency.setString(1, frequency.toString());
    this.updateDiplotypeFrequency.setString(2, diplotype.toString());
    int result = this.updateDiplotypeFrequency.executeUpdate();

    if (result == 0) {
      sf_logger.warn("Diplotype not found [{}]", diplotype.toString());
    } else if (result > 1) {
      throw new RuntimeException("More than 1 diplotype found [" + diplotype + "]");
    }
  }

  void updateAlleleFrequency(String alleleName, JsonObject frequency) throws Exception {
    this.updateAlleleFrequency.setString(1, frequency.toString());
    this.updateAlleleFrequency.setString(2, alleleName);
    int result = this.updateAlleleFrequency.executeUpdate();

    if (result == 0) {
      sf_logger.warn("Allele not found [{}]", alleleName);
    } else if (result > 1) {
      throw new RuntimeException("More than 1 allele found [" + alleleName + "]");
    }
  }

  void updateResultFrequency(String phenotype, JsonObject frequency) throws Exception {
    this.updateResultFrequency.setString(1, frequency.toString());
    this.updateResultFrequency.setString(2, phenotype);
    int result = this.updateResultFrequency.executeUpdate();

    if (result == 0) {
      throw new NotFoundException("Phenotype not found [" + phenotype + "]");
    } else if (result > 1) {
      throw new RuntimeException("More than 1 phenotype found [" + phenotype + "]");
    }
  }

  boolean isValidAllele(String alleleName) {
    return alleleNameMap.containsKey(alleleName);
  }

  private int getAuthorIdx() {
    return this.colStartOffset;
  }

  private int getPubYearIdx() {
    return this.colStartOffset + 1;
  }

  /**
   * Gets the column index for the PMID value
   */
  private int getPmidIdx() {
    return this.colStartOffset + 2;
  }

  /**
   * Gets the column index for the Ethnicity value
   */
  private int getEthIdx() {
    return this.colStartOffset + 3;
  }

  /**
   * Gets the column index for the Population value
   */
  private int getPopIdx() {
    return this.colStartOffset + 4;
  }

  /**
   * Gets the column index for the Population Info value
   */
  private int getPopInfoIdx() {
    return this.colStartOffset + 5;
  }

  /**
   * Gets the column index for the Subject Type value
   */
  private int getSubjTypeIdx() {
    return this.colStartOffset + 6;
  }

  /**
   * Gets the column index for the Subject Count (n) value
   */
  private int getNIdx() {
    return this.colStartOffset + 7;
  }

  LookupMethod getLookupMethod() {
    return this.lookupMethod;
  }
}
