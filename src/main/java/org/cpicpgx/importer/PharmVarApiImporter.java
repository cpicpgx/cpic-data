package org.cpicpgx.importer;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class to import PharmVar data from their API. This will find all alleles in PharmVar genes that don't have PharmVar
 * IDs assigned to them and attempt to find their current IDs.
 */
public class PharmVarApiImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String sf_pharmVarApi = "https://www.pharmvar.org/api-service";

  public static void main(String[] args) {
    try {
      PharmVarApiImporter.execute();
    } catch (IOException ex) {
      sf_logger.error("Error importing from PharmVar API", ex);
    } catch (SQLException ex) {
      sf_logger.error("Error with DB", ex);
    }
  }

  private static void execute() throws IOException, SQLException {
    OkHttpClient client = new OkHttpClient().newBuilder()
        .build();
    Gson gson = new Gson();
    Connection conn = ConnectionFactory.newConnection();

    String geneResponse = apiRequest(client, sf_pharmVarApi + "/genes/list");
    if (geneResponse == null) {
      throw new IOException("No response");
    }
    String[] geneList = gson.fromJson(geneResponse, String[].class);

    try (
        PreparedStatement geneListStmt = conn.prepareStatement("select a.genesymbol, a.name, a.id from allele_definition a where a.pharmvarId is null and a.genesymbol = any (?) order by a.genesymbol, a.name");
        PreparedStatement updateAllele = conn.prepareStatement("update allele_definition a set pharmvarId=? where id=?")
    ) {
      geneListStmt.setArray(1, conn.createArrayOf("TEXT", geneList));
      try (
          ResultSet rs = geneListStmt.executeQuery()
      ) {
        while (rs.next()) {
          String gene = rs.getString(1);
          String alleleName = rs.getString(2);
          int id = rs.getInt(3);

          String allelePath = UrlEscapers.urlPathSegmentEscaper().escape(gene + alleleName);

          String url = sf_pharmVarApi + "/alleles/" + allelePath + "/pvid";
          try {
            String alleleResponse = apiRequest(client, url);
            if (!StringUtils.isBlank(alleleResponse)) {
              String[] idArray = gson.fromJson(alleleResponse, String[].class);
              if (idArray.length == 1) {
                sf_logger.info("{} {} = PVID {}", gene, alleleName, idArray[0]);
                updateAllele.setString(1, idArray[0]);
                updateAllele.setInt(2, id);
                updateAllele.executeUpdate();
              } else {
                sf_logger.info("No ID found for {}", gene + alleleName);
              }
            }
          } catch (IOException ex) {
            sf_logger.warn("Request failed for {} {}: {}", gene, alleleName, url);
          }
        }
      }
    }
  }

  @Nullable
  private static String apiRequest(OkHttpClient client, String url) throws IOException {
    Request request = new Request.Builder().url(url).method("GET", null).build();
    try (Response response = client.newCall(request).execute()) {

      if (!response.isSuccessful()) {
        throw new IOException("Unexpected response " + response);
      }
      if (response.body() == null) {
        return null;
      }
      return response.body().string();
    }
  }
}
