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
  private static final String FILE_NAME = "cpic_gene_data_count.csv";
  
  private Path m_baseDirectory;
  private SortedSet<String> genes = new TreeSet<>();
  private Map<String, Map<String, Integer>> typeMap = new TreeMap<>();
  

  public static void main(String[] args) {
    ProgressReport report = new ProgressReport();
    try {
      report.parseArgs(args);
      report.gatherStats();
      report.writeFile();
    } catch (Exception e) {
      sf_logger.error("Error writing progress report", e);
    }
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
      queryData(conn, "select genesymbol,count(*) from allele group by genesymbol order by genesymbol","Allele Definition Table");
      queryData(conn, "select genesymbol, count(*) from function_reference r join allele a on r.alleleid = a.id group by genesymbol order by genesymbol","Allele Functionality Table");
      queryData(conn, "select genesymbol, count(*) from diplotype_view group by genesymbol order by genesymbol","Diplotype to Phenotype Table");
      queryData(conn, "select genesymbol, count(*) from allele_frequency f join allele a on f.alleleid = a.id group by genesymbol order by genesymbol","Frequency Table");
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
      typeMap.put(type, countMap);
    }
  }
  
  private void writeFile() throws IOException {
    Path filePath = m_baseDirectory.resolve(FILE_NAME);
    try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath.toFile()), CSVFormat.EXCEL)) {
      printer.print("");
      for (String g : genes) {
        printer.print(g);
      }
      printer.println();
      
      for (String t : typeMap.keySet()) {
        printer.print(t);
        Map<String,Integer> geneMap = typeMap.get(t);
        for (String g : genes) {
          if (geneMap.containsKey(g)) {
            printer.print(geneMap.get(g));
          } else {
            printer.print("");
          }
        }
        printer.println();
      }
    }
    sf_logger.info("Wrote report to {}", filePath);
  }
}
