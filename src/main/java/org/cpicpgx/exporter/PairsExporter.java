package org.cpicpgx.exporter;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.workbook.PairWorkbook;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class exports gene-drug pair information. It exports two views of pairs.
 *
 * @author Ryan Whaley
 */
public class PairsExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String sf_allPairQuery = "select " +
      "    p.genesymbol as \"Gene\", " +
      "    d.name as \"Drug\", " +
      "    g.url as \"Guideline URL\", " +
      "    p.cpiclevel as \"CPIC Level\", " +
      "    p.pgkbcalevel as \"PharmGKB Level of Evidence\", " +
      "    p.pgxtesting as \"PGx Level of Evidence\", " +
      "    array_to_string(p.citations, ';') as \"CPIC Publications (PMID)\", " +
      "    case " +
      "        when (p.usedforrecommendation and p.guidelineid is not null) then 'Yes' " +
      "        when (not p.usedforrecommendation and p.guidelineid is not null) then 'No' " +
      "        else 'n/a' end \"Used for Recommendation\", " +
      "    case when p.removed is true then 'Yes' else 'No' end \"Removed\", " +
      "    p.removeddate \"Date Removed\", " +
      "    p.removedreason \"Reason Removed\" " +
      "from pair p " +
      "         join drug d on p.drugid = d.drugid " +
      "         left join guideline g on p.guidelineid = g.id " +
      "order by p.cpiclevel, d.name, p.genesymbol";
  private static final String sf_changeLogQuery = "select date, note from change_log where type=?";
  private static final String sf_currentPairQuery = "select\n" +
      "       genesymbol as \"Gene\",\n" +
      "       drugname as \"Drug\",\n" +
      "       guidelineurl as \"Guideline\",\n" +
      "       cpiclevel as \"CPIC Level\",\n" +
      "       case when provisional is true then 'Provisional' else 'Final' end \"CPIC Level Status\",\n" +
      "       pgkbcalevel as \"PharmGKB Level of Evidence\",\n" +
      "       pgxtesting as \"PGx on FDA Label\",\n" +
      "       array_to_string(pmids, ';') as \"CPIC Publications (PMID)\"\n" +
      "from pair_view\n" +
      "order by cpiclevel, drugname, genesymbol";

  public static void main(String[] args) {
    try {
      PairsExporter export = new PairsExporter();
      export.parseArgs(args);
      export.export();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }

  public FileType getFileType() {
    return FileType.PAIR;
  }

  public void export() {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement changeStmt = conn.prepareStatement(sf_changeLogQuery)
    ) {
      // NOTE: this is a non-standard query so don't use the "queryChangeLog" method
      changeStmt.setString(1, FileType.PAIR.name());
      List<Object[]> changeLogEvents = new ArrayList<>();
      try (ResultSet rs = changeStmt.executeQuery()) {
        while (rs.next()) {
          changeLogEvents.add(new Object[]{rs.getDate(1), rs.getString(2)});
        }
      }

      try (PreparedStatement stmt = conn.prepareStatement(sf_allPairQuery)) {
        PairWorkbook pairWorkbook = new PairWorkbook(true);
        writeToFile(pairWorkbook, stmt, changeLogEvents);
      }

      try (PreparedStatement stmt = conn.prepareStatement(sf_currentPairQuery)) {
        PairWorkbook pairWorkbook = new PairWorkbook(false);
        writeToFile(pairWorkbook, stmt, changeLogEvents);
      }
    } catch (IOException e) {
      sf_logger.error("Couldn't write to filesystem", e);
    } catch (SQLException e) {
      sf_logger.error("Couldn't query the DB", e);
    } catch (Exception e) {
      sf_logger.error("Error making pairs", e);
    }
  }

  private void writeToFile(PairWorkbook pairWorkbook, PreparedStatement stmt, List<Object[]> changeLogEvents) throws Exception {
    pairWorkbook.writeHeader(stmt.getMetaData());
    try (ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        pairWorkbook.writePair(rs);
      }
    }
    pairWorkbook.writeChangeLog(changeLogEvents);
    addFileExportHistory(pairWorkbook.getFilename(), new String[]{});
    writeWorkbook(pairWorkbook);
    handleFileUpload();
  }
}
