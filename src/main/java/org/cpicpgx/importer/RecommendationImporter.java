package org.cpicpgx.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.util.Genotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer class for recommendation CSV tables.
 * 
 * This expects the CSV to follow these rules
 * 
 * <ol>
 *   <li>The name of the file must start with the drug name followed by a period then any other text is ignored</li>
 *   <li>File names must all end in ".csv"</li>
 *   <li>First row is a header</li>
 *   <li>First n columns are one for each n genes used to match the recommendation</li>
 *   <li>Each gene column has a header in the form "GENE_SYMBOL Phenotype"</li>
 *   <li>After gene column(s) are three columns: implication, recommendation, strength (header text doesn't matter but order does)</li>
 * </ol>
 *
 * @author Ryan Whaley
 */
public class RecommendationImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern DRUG_NAME_PATTERN = Pattern.compile("(\\w+).*\\.csv");

  public static void main(String[] args) {
    try {
      RecommendationImporter importer = new RecommendationImporter();
      importer.parseArgs(args);
      importer.execute();
    } catch (Exception ex) {
      sf_logger.error("Error importing recommendations", ex);
    }
  }
  
  private RecommendationImporter() {}
  
  public RecommendationImporter(String directory) {
    this.setDirectory(directory);
  }
  
  public void execute() {
    streamFiles()
        .filter(filterFileFunction(".csv"))
        .forEach(processFile);
  }
  
  private Consumer<File> processFile = f -> {
    Matcher m = DRUG_NAME_PATTERN.matcher(f.getName().toLowerCase());
    if (!m.find()) {
      sf_logger.warn("No drug name found for {}", f.getName().toLowerCase());
      return;
    }
    String drug = m.group(1);
    sf_logger.info(drug);

    List<String> geneList = new ArrayList<>();
    try (FileReader fileReader = new FileReader(f); DbHarness dbHarness = new DbHarness()) {
      CSVParser rows = CSVFormat.DEFAULT.parse(fileReader);
      for (CSVRecord row : rows) {
        if (row.getRecordNumber() == 1) {
          for (int i = 0; i < row.size(); i++) {
            String cellValue = row.get(i);
            if (cellValue.endsWith(" Phenotype") && i == geneList.size()) {
              String gene = cellValue.replaceAll(" Phenotype", "");
              geneList.add(gene);
              sf_logger.info("Header: Gene {}", gene);
            } else {
              sf_logger.info("Header: {}", cellValue);
            }
          }
        } else {
          int columnOffset = geneList.size(); // the next 3 columns after the genes are always the same
          Genotype genotype = new Genotype();
          for (int i = 0; i < columnOffset; i++) {
            genotype.with(geneList.get(i), row.get(i));
          }
         dbHarness.insert(drug, genotype, row.get(columnOffset), row.get(columnOffset + 1), row.get(columnOffset + 2));
        }
      }
    } catch (IOException e) {
      sf_logger.error("Error reading file", e);
    } catch (SQLException e) {
      sf_logger.error("Error writing to database", e);
    } catch (Exception e) {
      e.printStackTrace();
    }
  };
  
  private class DbHarness implements AutoCloseable {
    private Connection conn;
    private PreparedStatement insertStmt;
    private PreparedStatement drugLookupStmt;
    private PreparedStatement guidelineLookupStmt;
    private List<AutoCloseable> closables = new ArrayList<>();
    
    DbHarness() throws SQLException {
      this.conn = ConnectionFactory.newConnection();
      this.insertStmt = this.conn.prepareStatement("insert into recommendation(guidelineid, drugid, implications, drug_recommendation, classification, genotypes) values (?, ?, ?, ?, ? , ?::JSONB)");
      this.drugLookupStmt = this.conn.prepareStatement(
          "select drugid from drug where name=?", 
          ResultSet.TYPE_SCROLL_INSENSITIVE, 
          ResultSet.CONCUR_READ_ONLY);
      this.guidelineLookupStmt = this.conn.prepareStatement(
          "select id from guideline where name like ?", 
          ResultSet.TYPE_SCROLL_INSENSITIVE, 
          ResultSet.CONCUR_READ_ONLY);
      closables.add(this.guidelineLookupStmt);
      closables.add(this.drugLookupStmt);
      closables.add(this.insertStmt);
      closables.add(this.conn);
    }
    
    void insert(String drug, Genotype genotype, String implications, String recommendation, String classification) {
      try {
        String drugId = lookupDrug(drug);
        Long guidelineId = lookupGuideline(drug);

        sf_logger.info("Guideline:{}", guidelineId);
        sf_logger.info(drugId);
        sf_logger.info(genotype.toString());
        sf_logger.info(implications);
        sf_logger.info(recommendation);
        sf_logger.info(classification);
        
        this.insertStmt.setLong(1, guidelineId);
        this.insertStmt.setString(2, drugId);
        if (StringUtils.isNotBlank(implications)) {
          this.insertStmt.setString(3, implications);
        } else {
          this.insertStmt.setNull(3, Types.VARCHAR);
        }
        if (StringUtils.isNotBlank(recommendation)) {
          this.insertStmt.setString(4, recommendation);
        } else {
          this.insertStmt.setNull(4, Types.VARCHAR);
        }
        if (StringUtils.isNotBlank(classification)) {
          this.insertStmt.setString(5, classification);
        } else {
          this.insertStmt.setNull(5, Types.VARCHAR);
        }
        this.insertStmt.setObject(6, genotype.toString());
        
        this.insertStmt.executeUpdate();
        
      } catch (Exception e) {
        throw new RuntimeException("Error inserting record", e);
      }
    }
    
    private String lookupDrug(String name) throws SQLException, NotFoundException {
      this.drugLookupStmt.clearParameters();
      this.drugLookupStmt.setString(1, name);
      ResultSet rs = this.drugLookupStmt.executeQuery();
      if (rs.first()) {
        return rs.getString(1);
      } else {
        throw new NotFoundException("Couldn't find drug");
      }
    }

    private Long lookupGuideline(String name) throws SQLException, NotFoundException {
      this.guidelineLookupStmt.clearParameters();
      this.guidelineLookupStmt.setString(1, "%"+name+"%");
      ResultSet rs = this.guidelineLookupStmt.executeQuery();
      if (rs.first()) {
        if (!rs.isLast()) {
          throw new NotFoundException("Couldn't find just 1 guideline for " + name);
        }
        return rs.getLong(1);
      } else {
        throw new NotFoundException("Couldn't find guideline");
      }
    }

    @Override
    public void close() {
      closables.forEach(c -> {
        try {
          c.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
