package org.cpicpgx.exporter;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.db.ConnectionFactory;
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
 * Class to handle exporting a CPIC report out to filesystem.
 *
 * @author Ryan Whaley
 */
public class PairsExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String sf_pairQuery = "select " +
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
  //language=PostgreSQL
  private static final String sf_changeLogQuery = "select date, note from change_log where type=?";

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
        PreparedStatement stmt = conn.prepareStatement(sf_pairQuery);
        PreparedStatement changeStmt = conn.prepareStatement(sf_changeLogQuery)
    ) {
      PairWorkbook pairWorkbook = new PairWorkbook();
      pairWorkbook.writeHeader(stmt.getMetaData());
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          pairWorkbook.writePair(rs);
        }
      }


      // NOTE: this is a non-standard query so don't use the "queryChangeLog" method
      changeStmt.setString(1, FileType.PAIR.name());
      List<Object[]> changeLogEvents = new ArrayList<>();
      try (ResultSet rs = changeStmt.executeQuery()) {
        while (rs.next()) {
          changeLogEvents.add(new Object[]{rs.getDate(1), rs.getString(2)});
        }
      }
      pairWorkbook.writeChangeLog(changeLogEvents);

      addFileExportHistory(pairWorkbook.getFilename(), new String[]{});

      writeWorkbook(pairWorkbook);
      handleFileUpload();
    } catch (IOException e) {
      sf_logger.error("Couldn't write to filesystem", e);
    } catch (SQLException e) {
      sf_logger.error("Couldn't query the DB", e);
    } catch (Exception e) {
      sf_logger.error("Error making pairs", e);
    }
  }
}
