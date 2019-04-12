package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Export recommendation excel workbooks
 *
 * @author Ryan Whaley
 */
public class RecommendationExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    RecommendationExporter exporter = new RecommendationExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting recommendations", ex);
    }
  }
  
  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement drugStmt = conn.prepareStatement("select distinct r.drugid, d.name from recommendation r join drug d on r.drugid = d.drugid");
        PreparedStatement recStmt = conn.prepareStatement("select r.phenotypes, r.drug_recommendation, r.implications, r.classification from recommendation r where r.drugid=?");
        ResultSet drs = drugStmt.executeQuery()
    ) {
      while (drs.next()) {
        String drugId = drs.getString(1);
        String drugName = drs.getString(2);
        
        sf_logger.info("Processing {} {}", drugId, drugName);
        RecommendationWorkbook workbook = new RecommendationWorkbook(drugName);
        
        recStmt.setString(1, drugId);
        try (ResultSet rrs = recStmt.executeQuery()) {
          while (rrs.next()) {
            String phenotypes = rrs.getString(1);
            String recommendation = rrs.getString(2);
            String implication = rrs.getString(3);
            String classification = rrs.getString(4);
            
            workbook.writeRec(phenotypes, recommendation, implication, classification);
          }
        }
        
        writeWorkbook(workbook);
      }
    }
  }
}
