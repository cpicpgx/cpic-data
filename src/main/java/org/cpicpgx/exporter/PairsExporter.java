package org.cpicpgx.exporter;

import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class to handle exporting a CPIC report out to filesystem.
 *
 * @author Ryan Whaley
 */
public class PairsExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String sf_defaultFileName = "cpicPairs.csv";
  private static final String sf_pairQuery = "select " +
      "p.geneSymbol as \"Gene\", " +
      "d2.name as \"Drug\", " +
      "g.url as \"Guideline URL\", " +
      "level as \"CPIC Level\", " +
      "pgkbcalevel as \"PharmGKB Level of Evidence\", " +
      "pgxtesting as \"PGx Level of Evidence\", " +
      "array_to_string(citations, ';') as \"CPIC Publications (PMID)\" " +
      "from pair p " +
      "join drug d2 on p.drugid = d2.drugid " +
      "left join guideline g on p.guidelineid = g.id " +
      "order by p.level, d2.name";

  public static void main(String[] args) {
    try {
      PairsExporter export = new PairsExporter();
      export.parseArgs(args);
      export.export();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }

  FileType getFileType() {
    return FileType.PAIRS;
  }

  EntityType getEntityCategory() {
    return EntityType.PAIR;
  }

  public void export() {
    sf_logger.debug("Will write to " + this.directory);
    
    Path pairsFile = this.directory.resolve(sf_defaultFileName);
    try (Connection conn = ConnectionFactory.newConnection()) {
      try (
          FileWriter fw = new FileWriter(pairsFile.toFile());
          PreparedStatement stmt = conn.prepareStatement(sf_pairQuery)
      ) {
        CSVPrinter csvPrinter = CSVFormat.DEFAULT.withHeader(stmt.getMetaData()).print(fw);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            csvPrinter.printRecord(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3),
                rs.getString(4),
                rs.getString(5),
                rs.getString(6),
                rs.getString(7)
            );
          }
        }
      }
      addExportEvent(conn);
    } catch (IOException e) {
      sf_logger.error("Couldn't write to filesystem", e);
    } catch (SQLException e) {
      sf_logger.error("Couldn't query the DB", e);
    }
    
    sf_logger.info("Wrote pairs to: {}", pairsFile);
  }
}
