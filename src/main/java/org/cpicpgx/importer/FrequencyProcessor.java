package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.cpicpgx.db.ConnectionFactory;
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
  
  private String gene;
  private Connection conn;
  private Map<String, Long> alleleNameMap = new HashMap<>();
  private Map<Integer, Long> colIdxAlleleIdMap = new HashMap<>();

  private PreparedStatement insertStatement;
  private PreparedStatement insertPopulation;

  /**
   * Construct the FrequencyProcessor
   * @param gene the Gene symbol for the gene this frequency data is for
   * @param headerRow the header Row from the frequency data sheet
   * @throws SQLException can occur when reading the header row
   */
  FrequencyProcessor(String gene, RowWrapper headerRow) throws SQLException {
    this.gene = gene;
    this.conn = ConnectionFactory.newConnection();

    PreparedStatement pstmt = this.conn.prepareStatement("select name, id from allele where allele.hgncid=?");
    pstmt.setString(1, this.gene);
    ResultSet rs = pstmt.executeQuery();
    while (rs.next()) {
      this.alleleNameMap.put(rs.getString(1), rs.getLong(2));
    }

    this.insertStatement = 
        this.conn.prepareStatement("insert into allele_frequency(alleleid, population, frequency, label) values (?, ?, ?, ?)");
    this.insertPopulation = 
        this.conn.prepareStatement("insert into population(ethnicity, population, populationinfo, subjecttype, subjectcount, citation) values (?, ?, ?, ?, ?, ?) returning (id)");
    
    for (short i = headerRow.row.getFirstCellNum(); i < headerRow.row.getLastCellNum(); i++) {
      String cellText = headerRow.getNullableText(i);
      if (StringUtils.isNotBlank(cellText) && alleleNameMap.keySet().contains(cellText)) {
        colIdxAlleleIdMap.put((int)i, alleleNameMap.get(cellText));
        sf_logger.debug("Will get {} frequencies from column {}", cellText, i);
      }
    }
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
    if (!row.hasTextIn(4)) return;
    
    this.insertPopulation.setString(1, row.getNullableText(4));
    this.insertPopulation.setString(2, row.getNullableText(5));
    this.insertPopulation.setString(3, row.getNullableText(6));
    this.insertPopulation.setString(4, row.getNullableText(7));
    this.insertPopulation.setLong(5, row.getNullableLong(8));
    this.insertPopulation.setString(6, row.getNullableText(3));
    
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
}
