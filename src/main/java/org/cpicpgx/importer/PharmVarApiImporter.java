package org.cpicpgx.importer;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.cpicpgx.util.HttpUtils.apiRequest;

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
    } catch (IOException|NotFoundException ex) {
      sf_logger.error("Error importing from PharmVar API", ex);
    } catch (SQLException ex) {
      sf_logger.error("Error with DB", ex);
    }
  }

  public static void execute() throws IOException, SQLException, NotFoundException {
    OkHttpClient client = new OkHttpClient().newBuilder()
        .build();
    Gson gson = new Gson();

    String geneResponse = apiRequest(client, sf_pharmVarApi + "/genes/list");
    if (geneResponse == null) {
      throw new IOException("No response");
    }
    String[] geneList = gson.fromJson(geneResponse, String[].class);

    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement geneListStmt = conn.prepareStatement("select a.genesymbol, a.name, a.id from allele_definition a where a.pharmvarId is null and a.genesymbol = any (?) order by a.genesymbol, a.name");
        PreparedStatement updateAllele = conn.prepareStatement("update allele_definition a set pharmvarId=? where id=?")
    ) {
      geneListStmt.setArray(1, conn.createArrayOf("TEXT", geneList));
      SortedSet<String> updatedGenes = new TreeSet<>();
      try (
          ResultSet rs = geneListStmt.executeQuery()
      ) {
        while (rs.next()) {
          String gene = rs.getString(1);
          String alleleName = rs.getString(2);
          int id = rs.getInt(3);

          try {
            String alleleResponse = requestFromPharmVar(client, gene, alleleName);
            if (!StringUtils.isBlank(alleleResponse)) {
              String[] idArray = gson.fromJson(alleleResponse, String[].class);
              if (idArray.length == 1) {
                sf_logger.debug("{} {} = PVID {}", gene, alleleName, idArray[0]);
                updateAllele.setString(1, idArray[0]);
                updateAllele.setInt(2, id);
                updateAllele.executeUpdate();
                updatedGenes.add(gene);
              } else {
                sf_logger.debug("No ID found for {}", gene + alleleName);
              }
            } else {
              throw new NotFoundException("Got an empty response for " + gene + alleleName);
            }
          } catch (IOException ex) {
            sf_logger.debug("Request failed for {} {}", gene, alleleName);
          } catch (NotFoundException ex) {
            sf_logger.warn("Got no response from PharmVar for {} {}, continuing", gene, alleleName);
          }
        }
      }
      if (!updatedGenes.isEmpty()) {
        sf_logger.info("Processed {}", String.join("; ", updatedGenes));
      }
    }
  }

  /**
   * Get PharmVar's record for a gene-allele and retry a commonly used allele name format if the "normal" format gives
   * no result. The retry will only work for alleles in the format "*\d+"
   * @param client an {@link OkHttpClient}
   * @param gene the gene to request
   * @param alleleName the allele name to request (e.g. *3)
   * @return the String content of the HTTP response
   * @throws IOException can occur from netowrk IO
   * @throws NotFoundException can occur if PharmVar has no record for the allele
   */
  private static String requestFromPharmVar(OkHttpClient client, String gene, String alleleName) throws IOException, NotFoundException {
    boolean ableToRetry = alleleName.matches("\\*\\d+");
    String allelePath = UrlEscapers.urlPathSegmentEscaper().escape(gene + alleleName);
    String url = sf_pharmVarApi + "/alleles/" + allelePath + "/pvid";
    String response = null;
    try {
      response = apiRequest(client, url);
    } catch (NotFoundException ex) {
      if (ableToRetry) {
        // if not no response and allele is in typical format, we can retry with a common suffix
        String retryPath = UrlEscapers.urlPathSegmentEscaper().escape(gene + alleleName + ".001");
        String retryUrl = sf_pharmVarApi + "/alleles/" + retryPath + "/pvid";
        response = apiRequest(client, retryUrl);
      }
    }
    return response;
  }
}
