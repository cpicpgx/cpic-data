package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
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
public class FrequencyProcessor implements AutoCloseable {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private Connection conn;
  private Map<Integer, Long> colIdxAlleleIdMap = new HashMap<>();
  private int colStartOffset = 0;
  

  private PreparedStatement insertStatement;
  private PreparedStatement insertPopulation;

  /**
   * Construct the FrequencyProcessor
   * @param gene the Gene symbol for the gene this frequency data is for
   * @param headerRow the header Row from the frequency data sheet
   * @throws SQLException can occur when reading the header row
   */
  FrequencyProcessor(String gene, RowWrapper headerRow) throws SQLException, NotFoundException {
    this.conn = ConnectionFactory.newConnection();

    clearRecords(gene);

    PreparedStatement pstmt = this.conn.prepareStatement("select name, id from allele where allele.geneSymbol=?");
    pstmt.setString(1, gene);
    ResultSet rs = pstmt.executeQuery();
    Map<String, Long> alleleNameMap = new HashMap<>();
    while (rs.next()) {
      alleleNameMap.put(rs.getString(1), rs.getLong(2));
    }

    this.insertStatement = 
        this.conn.prepareStatement("insert into allele_frequency(alleleid, population, frequency, label) values (?, ?, ?, ?)");
    this.insertPopulation = 
        this.conn.prepareStatement("insert into population(ethnicity, population, populationinfo, subjecttype, subjectcount, citation) values (?, ?, ?, ?, ?, ?) returning (id)");
    
    for (short i = headerRow.row.getFirstCellNum(); i < headerRow.row.getLastCellNum(); i++) {
      String cellText = headerRow.getNullableText(i);
      if (StringUtils.isNotBlank(cellText) && cellText.contains("Authors")) {
        colStartOffset = i;
        sf_logger.debug("Will offset authors at column {}", i);
        sf_logger.debug("Will get pmid at column {}", getPmidIdx());
        sf_logger.debug("Will get N at column {}", getNIdx());
      }
      if (StringUtils.isNotBlank(cellText) && alleleNameMap.keySet().contains(cellText)) {
        colIdxAlleleIdMap.put((int)i, alleleNameMap.get(cellText));
        sf_logger.debug("Will get {} frequencies from column {}", cellText, i);
      }
    }
    
    if (colIdxAlleleIdMap.size() == 0) {
      throw new NotFoundException("No allele columns could be found for alleles " + String.join("; ", alleleNameMap.keySet()));
    }
    
    clearUnusedPopulations();
  }

  private void clearRecords(String gene) throws SQLException {
    int delCount = 0;
    try (PreparedStatement stmt = this.conn.prepareStatement("delete from allele_frequency where alleleid in (select id from allele where genesymbol=?)")) {
      stmt.setString(1, gene);
      delCount += stmt.executeUpdate();
    }
    sf_logger.info("cleared {} rows for {}", delCount, gene);
  }

  private void clearUnusedPopulations() throws SQLException {
    int delCount = 0;
    try (PreparedStatement stmt = this.conn.prepareStatement("delete from population where id not in (select population from allele_frequency)")) {
      delCount += stmt.executeUpdate();
    }
    sf_logger.info("cleared {} unused population records", delCount);
  }

  /**
   * Read frequency data from the given row at the given column index and writes it to the DB
   * @param row a row of frequency data
   * @param alleleColIdx the index of the allele column to read
   * @param populationId the ID of the population DB record this frequency is for
   * @throws SQLException can occur when writing to the DB
   */
  private void insertFrequency(RowWrapper row, Integer alleleColIdx, Long populationId) throws SQLException {
    if (alleleColIdx == null || !colIdxAlleleIdMap.keySet().contains(alleleColIdx)) {
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
    
    this.insertPopulation.setString(1, row.getNullableText(getEthIdx()));
    this.insertPopulation.setString(2, row.getNullableText(getPopIdx()));
    this.insertPopulation.setString(3, row.getNullableText(getPopInfoIdx()));
    this.insertPopulation.setString(4, row.getNullableText(getSubjTypeIdx()));
    this.insertPopulation.setLong(5, row.getNullableLong(getNIdx()));
    this.insertPopulation.setString(6, row.getNullableText(getPmidIdx()));
    
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

  @Override
  public void close() throws Exception {
    if (conn != null && !conn.isClosed()) {
      conn.close();
    }
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
}
