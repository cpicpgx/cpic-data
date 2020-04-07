package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * A class that will export one file per Gene that has example CDS data
 *
 * @author Ryan Whaley
 */
public class GeneCdsExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    GeneCdsExporter exporter = new GeneCdsExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception e) {
      sf_logger.error("Error exporting gene CDS information", e);
    }
  }

  FileType getFileType() {
    return FileType.GENE_CDS;
  }

  EntityType getEntityCategory() {
    return EntityType.GENE;
  }
  
  @Override
  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement geneStmt = conn.prepareStatement("select distinct p.genesymbol from gene_phenotype p");
        ResultSet geneRs = geneStmt.executeQuery();
        PreparedStatement cdsStmt = conn.prepareStatement("select phenotype, ehrpriority, consultationtext, notes from gene_phenotype where genesymbol=?")
    ) {
      while (geneRs.next()) {
        String geneSymbol = geneRs.getString(1);
        GeneCdsWorkbook workbook = new GeneCdsWorkbook(geneSymbol);
        
        cdsStmt.setString(1, geneSymbol);
        try (ResultSet cdsRs = cdsStmt.executeQuery()) {
          while (cdsRs.next()) {
            workbook.writeConsultation(
                cdsRs.getString(1),
                cdsRs.getString(2),
                cdsRs.getString(3),
                cdsRs.getString(4)
            );
          }
        }
        
        workbook.writeNotes(queryGeneNotes(conn, geneSymbol, NoteType.CDS));

        writeWorkbook(workbook);
      }
      handleFileUpload();
      addExportEvent(conn);
    }
  }
}
