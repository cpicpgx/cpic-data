package org.cpicpgx.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

  public FileType getFileType() {
    return FileType.RECOMMENDATION;
  }

  EntityType getEntityCategory() {
    return EntityType.DRUG;
  }
  
  public void export() throws Exception {
    Gson gson = new Gson();
    //noinspection UnstableApiUsage
    Type stringMapType = new TypeToken<TreeMap<String, String>>(){}.getType();

    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement drugStmt = conn.prepareStatement("select distinct r.drugid, d.name from recommendation r join drug d on r.drugid = d.drugid");
        PreparedStatement geneStmt = conn.prepareStatement("select distinct jsonb_object_keys(phenotypes) from recommendation where drugid=?");
        PreparedStatement popStmt = conn.prepareStatement("select distinct population from recommendation where drugid=?");
        PreparedStatement recStmt = conn.prepareStatement("select r.phenotypes, r.drugRecommendation, r.implications, r.classification, r.activityScore, r.comments from recommendation r where r.drugid=? and r.population=?");
        PreparedStatement changeStmt = conn.prepareStatement("select n.date, n.note from change_log n where entityId=? and type=? order by date");
        ResultSet drs = drugStmt.executeQuery()
    ) {
      while (drs.next()) {
        String drugId = drs.getString(1);
        String drugName = drs.getString(2);

        Set<String> geneSymbols = new TreeSet<>();
        geneStmt.setString(1, drugId);
        try (ResultSet grs = geneStmt.executeQuery()) {
          while (grs.next()) {
            geneSymbols.add(grs.getString(1));
          }
        }
        
        sf_logger.info("Processing {} {}", drugId, drugName);
        RecommendationWorkbook workbook = new RecommendationWorkbook(drugName, geneSymbols);

        popStmt.setString(1, drugId);
        try (ResultSet prs = popStmt.executeQuery()) {
          while (prs.next()) {
            String population = prs.getString(1);
            workbook.setupSheet(population);

            recStmt.setString(1, drugId);
            recStmt.setString(2, population);

            try (ResultSet rrs = recStmt.executeQuery()) {
              while (rrs.next()) {
                String phenotypes = rrs.getString(1);
                String recommendation = rrs.getString(2);
                String implication = rrs.getString(3);
                String classification = rrs.getString(4);
                String activityScore = rrs.getString(5);
                String comments = rrs.getString(6);

                workbook.writeRec(
                    gson.fromJson(phenotypes, stringMapType),
                    gson.fromJson(activityScore, stringMapType),
                    gson.fromJson(implication, stringMapType),
                    recommendation,
                    classification,
                    comments
                );
              }
            }
          }
        }

        changeStmt.setString(1, drugId);
        changeStmt.setString(2, FileType.RECOMMENDATION.name());
        try (ResultSet rs = changeStmt.executeQuery()) {
          while (rs.next()) {
            workbook.writeHistory(rs.getDate(1), rs.getString(2));
          }
        }

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{drugId});
      }
      handleFileUpload();
    }
  }
}
