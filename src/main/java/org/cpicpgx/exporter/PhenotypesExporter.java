package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Export gene phenotypes excel workbooks
 *
 * @author Ryan Whaley
 */
public class PhenotypesExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    PhenotypesExporter exporter = new PhenotypesExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting gene phenotypes", ex);
    }
  }

  public FileType getFileType() {
    return FileType.GENE_PHENOTYPE;
  }

  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement stmt = conn.prepareStatement("select distinct p.genesymbol from gene_phenotype p join phenotype_function pf on p.id = pf.phenotypeid order by 1");
        ResultSet rs = stmt.executeQuery();
        PreparedStatement pstmt = conn.prepareStatement("select function1, function2, activityvalue1, activityvalue2, totalactivityscore, phenotype, description\n" +
            "from gene_phenotype p join phenotype_function pf on p.id = pf.phenotypeid\n" +
            "where p.genesymbol=?")
    ) {
      while (rs.next()) {
        String gene = rs.getString(1);
        PhenotypesWorkbook phenotypesWorkbook = new PhenotypesWorkbook(gene);

        pstmt.setString(1, gene);
        try (ResultSet pRs = pstmt.executeQuery()) {
          while (pRs.next()) {
            phenotypesWorkbook.writePhenotype(
                pRs.getString(1),
                pRs.getString(2),
                pRs.getString(3),
                pRs.getString(4),
                pRs.getString(5),
                pRs.getString(6),
                pRs.getString(7)
            );
          }
        }

        phenotypesWorkbook.writeChangeLog(queryChangeLog(conn, gene, getFileType()));

        writeWorkbook(phenotypesWorkbook);
        addFileExportHistory(phenotypesWorkbook.getFilename(), new String[]{gene});
      }
      handleFileUpload();
    }
  }
}
