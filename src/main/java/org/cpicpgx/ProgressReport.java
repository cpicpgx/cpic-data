package org.cpicpgx;

import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.cpicpgx.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This will export a CSV file with counts, by gene, of how much data is in each type of data table.  
 *
 * @author Ryan Whaley
 */
public class ProgressReport {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String GENE_FILE_NAME = "cpic_gene_data_count.csv";
  private static final String DRUG_FILE_NAME = "cpic_drug_data_count.csv";
  
  private Path m_baseDirectory;
  private final SortedSet<String> genes = new TreeSet<>();
  private final SortedSet<String> drugs = new TreeSet<>();
  private final Map<String, Map<String, Integer>> geneDataMap = new TreeMap<>();
  private final Map<String, Map<String, Integer>> drugDataMap = new TreeMap<>();
  

  public static void main(String[] args) {
    ProgressReport report = new ProgressReport();
    try {
      report.parseArgs(args);
      report.gatherStats();
      report.gatherDrugStats();
      report.writeFiles();
    } catch (Exception e) {
      sf_logger.error("Error writing progress report", e);
    }
  }
  
  private void writeFiles() throws IOException {
    writeFile(GENE_FILE_NAME, genes, geneDataMap);
    writeFile(DRUG_FILE_NAME, drugs, drugDataMap);
  }

  private void parseArgs(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", true,"path to directory to write files to");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cli = clParser.parse(options, args);

    m_baseDirectory = Paths.get(cli.getOptionValue("d"));
  }

  private void gatherStats() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      queryData(conn,
          //language=PostgreSQL
          "select genesymbol,count(*) from allele group by genesymbol order by genesymbol",
          "Allele Definition Table");
      queryData(conn,
          //language=PostgreSQL
          "select genesymbol, count(*) from allele a where clinicalfunctionalstatus is not null group by genesymbol order by genesymbol",
          "Allele Functionality Table");
      queryData(conn,
          //language=PostgreSQL
          "select genesymbol, count(*) from diplotype group by genesymbol order by genesymbol",
          "Diplotype to Phenotype Table");
      queryData(conn,
          //language=PostgreSQL
          "select genesymbol, count(*) from allele_frequency f join allele a on f.alleleid = a.id group by genesymbol order by genesymbol",
          "Frequency Table");
      queryData(conn,
          //language=PostgreSQL
          "select genesymbol, count(*) from gene_result where consultationtext is not null group by genesymbol",
          "Gene CDS Text");
      queryData(conn,
          //language=PostgreSQL
          "select g.genesymbol, count(*) from gene_result g join gene_result_lookup pf on g.id = pf.phenotypeid group by genesymbol",
          "Gene Phenotypes");
      queryData(conn,
          //language=PostgreSQL
          "select a.genesymbol, count(*) from allele_definition a where a.pharmvarid is not null group by a.genesymbol",
          "PharmVar Allele IDs Loaded");
    }
  }
  
  private void gatherDrugStats() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      queryDrugData(conn, "select name, count(*) from drug where flowchart is not null group by name order by name", "Flowcharts");
      queryDrugData(conn, "select d.name, count(distinct r.id) from recommendation r join drug d on r.drugid = d.drugid group by d.name", "Table 2 Recommendations");
      queryDrugData(conn, "select d.name, count(distinct t.id) from test_alert t join drug d on t.drugid = d.drugid group by d.name", "Drug Test Alerts");
      queryDrugData(conn, "select d.name, count(distinct g.id) from guideline g join pair p on g.id = p.guidelineid join drug d on p.drugid = d.drugid group by d.name", "Guideline");
    }
  }
  
  private void queryData(Connection conn, String query, String type) throws SQLException {
    try (PreparedStatement geneStmt = conn.prepareStatement(query);
         ResultSet grs = geneStmt.executeQuery()
    ) {
      Map<String,Integer> countMap = new HashMap<>();
      while (grs.next()) {
        genes.add(grs.getString(1));
        countMap.put(grs.getString(1), grs.getInt(2));
      }
      geneDataMap.put(type, countMap);
    }
  }
  
  private void queryDrugData(Connection conn, String query, String type) throws SQLException {
    try (PreparedStatement geneStmt = conn.prepareStatement(query);
         ResultSet grs = geneStmt.executeQuery()
    ) {
      Map<String,Integer> countMap = new HashMap<>();
      while (grs.next()) {
        drugs.add(grs.getString(1));
        countMap.put(grs.getString(1), grs.getInt(2));
      }
      drugDataMap.put(type, countMap);
    }
  }
  
  private void writeFile(String fileName, Collection<String> objects, Map<String, Map<String, Integer>> dataMap) throws IOException {
    Path filePath = m_baseDirectory.resolve(fileName);
    try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath.toFile()), CSVFormat.EXCEL)) {
      printer.print("");
      Map<String,Integer> totalCounts = new HashMap<>();
      for (String g : dataMap.keySet()) {
        printer.print(g);
        totalCounts.put(g, 0);
      }
      printer.println();
      
      for (String g : objects) {
        printer.print(g);
        for (String t : dataMap.keySet()) {
          Integer count = dataMap.get(t).get(g);
          if (count != null && count > 0) {
            printer.print("✅");
            totalCounts.put(t, totalCounts.get(t) + 1);
          } else {
            printer.print("");
          }
        }
        printer.println();
      }
      
      printer.print("Totals");
      for (String t : dataMap.keySet()) {
        printer.print(totalCounts.get(t));
      }
      printer.println();
    }
    sf_logger.info("Wrote report to {}", filePath);
  }
}
