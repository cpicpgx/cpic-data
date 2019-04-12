package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Exports files, by gene, of the diplotype to phenotype assignments.
 *
 * @author Ryan Whaley
 */
public class DiplotypePhenotypeExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    DiplotypePhenotypeExporter exporter = new DiplotypePhenotypeExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error diplotype phenotype", ex);
    }
  }
  
  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select distinct geneSymbol from diplotype_view");
         PreparedStatement dipStmt = conn.prepareStatement("select d.diplotype, d.phenotype, d.ehrpriority, d.activityscore from diplotype_view d where d.genesymbol=? order by d.diplotype");
         PreparedStatement phenoStmt = conn.prepareStatement("select g.phenotype, g.ehrpriority, g.consultationtext from gene_phenotype g where genesymbol=? and (g.ehrpriority is not null or g.consultationtext is not null)");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String gene = grs.getString(1);
        DiplotypeWorkbook workbook = new DiplotypeWorkbook(gene);
        
        dipStmt.setString(1, gene);
        try (ResultSet rs = dipStmt.executeQuery()) {
          while (rs.next()) {
            String diplotype = rs.getString(1);
            String phenotype = rs.getString(2);
            String ehr = rs.getString(3);
            
            int activityScore = rs.getInt(4);
            String activity = "N/A";
            if (!rs.wasNull()) {
              activity = String.valueOf(activityScore);
            }

            workbook.writeDiplotype(diplotype, phenotype, ehr, activity);
          }
        }

        phenoStmt.setString(1, gene);
        try (ResultSet rs = phenoStmt.executeQuery()) {
          while (rs.next()) {
            workbook.writeInterpretation(
                rs.getString(1),
                rs.getString(2),
                rs.getString(3)
            );
          }
        }
        
        writeWorkbook(workbook);
      }
    }
  }
}
