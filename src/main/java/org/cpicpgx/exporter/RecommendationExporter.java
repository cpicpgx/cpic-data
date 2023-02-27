package org.cpicpgx.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.workbook.RecommendationWorkbook;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

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

  public void export() throws Exception {
    Gson gson = new Gson();
    Type stringMapType = new TypeToken<TreeMap<String, String>>(){}.getType();

    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement drugStmt = conn.prepareStatement("select distinct r.drugid, d.name from recommendation r join drug d on r.drugid = d.drugid");
        PreparedStatement geneStmt = conn.prepareStatement("with r as ( " +
            "    select jsonb_object_keys(phenotypes) as genesymbol " +
            "    from recommendation " +
            "    where drugid = ? " +
            "    union " +
            "    select jsonb_object_keys(activityscore) as genesymbol " +
            "    from recommendation " +
            "    where drugid = ? " +
            "    union " +
            "    select jsonb_object_keys(allelestatus) as genesymbol " +
            "    from recommendation " +
            "    where drugid = ? " +
            ") select r.genesymbol, g.lookupmethod from r join gene g on (r.genesymbol=g.symbol)");
        PreparedStatement popStmt = conn.prepareStatement("select distinct population from recommendation where drugid=?");
        PreparedStatement recStmt = conn.prepareStatement("select r.phenotypes, r.drugRecommendation, r.implications, r.classification, r.activityScore, r.comments, r.allelestatus from recommendation r where r.drugid=? and r.population=?");
        ResultSet drs = drugStmt.executeQuery()
    ) {
      while (drs.next()) {
        String drugId = drs.getString(1);
        String drugName = drs.getString(2);

        Map<String, LookupMethod> geneLookupMap = new TreeMap<>();
        geneStmt.setString(1, drugId);
        geneStmt.setString(2, drugId);
        geneStmt.setString(3, drugId);
        try (ResultSet grs = geneStmt.executeQuery()) {
          while (grs.next()) {
            geneLookupMap.put(grs.getString(1), LookupMethod.valueOf(grs.getString(2)));
          }
        }
        
        sf_logger.debug("Processing {} {}", drugId, drugName);
        RecommendationWorkbook workbook = new RecommendationWorkbook(drugName, geneLookupMap);

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
                String implication = rrs.getString(3);
                String activityScore = rrs.getString(5);
                String comments = rrs.getString(6);
                String alleleStatus = rrs.getString(7);

                workbook.writeRec(
                    gson.fromJson(phenotypes, stringMapType),
                    gson.fromJson(activityScore, stringMapType),
                    gson.fromJson(implication, stringMapType),
                    gson.fromJson(alleleStatus, stringMapType),
                    rrs.getString(2),
                    rrs.getString(4),
                    comments
                );
              }
            }
          }
        }

        workbook.writeChangeLog(queryChangeLog(conn, drugId, getFileType()));

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{drugId});
      }
      handleFileUpload();
    }
  }
}
