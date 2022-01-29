package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.workbook.GeneResourceWorkbook;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;

/**
 * Exports individual excel sheets for each gene to show their links to external resources
 *
 * @author Ryan Whaley
 */
public class GeneResourceExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    GeneResourceExporter exporter = new GeneResourceExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting gene resource files", ex);
    }
  }

  public FileType getFileType() {
    return FileType.GENE_RESOURCE;
  }

  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select distinct g.symbol, g.hgncid, g.ncbiid, " +
             "g.ensemblid, g.pharmgkbid from gene g order by 1");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String symbol = grs.getString(1);
        String hgnc = grs.getString(2);
        String ncbi = grs.getString(3);
        String ensembl = grs.getString(4);
        String pgkb = grs.getString(5);
        
        GeneResourceWorkbook workbook = new GeneResourceWorkbook(symbol);
        workbook.writeIds(hgnc, ncbi, ensembl, pgkb);

        workbook.writeChangeLog(Collections.emptyList());

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{symbol});
      }
      handleFileUpload();
    }
  }
}
