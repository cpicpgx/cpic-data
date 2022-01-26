package org.cpicpgx.exporter;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.cpicpgx.util.HttpUtils.apiRequest;
import static org.cpicpgx.util.HttpUtils.buildPgkbUrl;

/**
 * This class will create Gene Resource files for genes by their symbol. This will query the PharmGKB API and match
 * genes to their primary symbol and then write the found data into a Gene Resource file. The file should be processed by
 * the {@link org.cpicpgx.importer.GeneReferenceImporter} class to add the gene to the DB.
 */
public class GeneResourceCreator {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("n", true,"1 or more gene symbols separated by semi-colons (;)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      String geneSymbols = cli.getOptionValue("n");

      GeneResourceCreator geneResourceCreator = new GeneResourceCreator(Path.of("out"));
      for (String gene : geneSymbols.split(";")) {
        geneResourceCreator.create(gene);
      }

      if (geneResourceCreator.getNoGeneFoundSet().size() > 0) {
        sf_logger.warn("Import these genes to PharmGKB: " + String.join("; ", geneResourceCreator.getNoGeneFoundSet()));
      }

      if (geneResourceCreator.getLackingDataMap().size() > 0) {
        sf_logger.warn("Fill in more PharmGKB data for:");
        for (String name : geneResourceCreator.getLackingDataMap().keySet()) {
          sf_logger.warn("{} = {}", name, geneResourceCreator.getLackingDataMap().get(name));
        }
      }
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
      System.exit(1);
    } catch (Exception e) {
      sf_logger.error("Error during import", e);
      System.exit(1);
    }
  }

  private final Set<String> noGeneFoundSet = new TreeSet<>();
  private final Map<String,String> lackingDataMap = new TreeMap<>();
  private final OkHttpClient f_httpClient;
  private final Gson f_gson;
  private final Path f_outputDir;

  /**
   *
   */
  public GeneResourceCreator(Path outputDir) {
    Preconditions.checkArgument(outputDir.toFile().exists(), "Output dir does not exist");
    Preconditions.checkArgument(outputDir.toFile().isDirectory(), "Output dir is not a directory");
    f_httpClient = new OkHttpClient().newBuilder()
        .build();
    f_gson = new Gson();
    f_outputDir = outputDir;
  }

  public void create(String rawSymbol) throws Exception {
    Thread.sleep(HttpUtils.API_WAIT_TIME);
    String symbol = StringUtils.strip(rawSymbol);

    String response;
    try {
      response = apiRequest(f_httpClient, buildPgkbUrl("data/gene", "view", "max", "symbol", symbol));
    } catch (Exception ex) {
      sf_logger.warn("No data found for {}: {}", symbol, ex.getMessage());
      if (ex instanceof NotFoundException) {
        noGeneFoundSet.add(symbol);
      }
      return;
    }

    if (StringUtils.isBlank(response)) {
      sf_logger.warn("No data found for {}", symbol);
      return;
    }

    JsonObject responseJson = f_gson.fromJson(response, JsonObject.class);
    JsonArray dataArray = responseJson.getAsJsonArray("data");
    if (dataArray.size() > 1) {
      sf_logger.warn("More than one gene found for {}", symbol);
      return;
    }

    JsonObject geneJson = dataArray.get(0).getAsJsonObject();

    String pharmgkbId = geneJson.get("id").getAsString();
    String hgncId = null;
    String ncbiId = null;
    String ensemblId = null;

    JsonArray xrefArray = geneJson.getAsJsonArray("crossReferences");
    if (xrefArray != null) {
      for (JsonElement xrefElement : xrefArray) {
        String resource = xrefElement.getAsJsonObject().get("resource").getAsString();
        switch (resource) {
          case "NCBI Gene":
            ncbiId = xrefElement.getAsJsonObject().get("resourceId").getAsString();
            break;
          case "Ensembl":
            ensemblId = xrefElement.getAsJsonObject().get("resourceId").getAsString();
            break;
          case "HGNC":
            hgncId = xrefElement.getAsJsonObject().get("resourceId").getAsString();
            break;
        }
      }
    }

    if (hgncId == null && ncbiId == null && ensemblId == null) {
      lackingDataMap.put(symbol, pharmgkbId);
    } else {
      GeneResourceWorkbook workbook = new GeneResourceWorkbook(symbol);
      workbook.writeIds(hgncId, ncbiId, ensemblId, pharmgkbId);
      workbook.getSheets().forEach(SheetWrapper::autosizeColumns);
      Path filePath = f_outputDir.resolve(workbook.getFilename());
      try (OutputStream fo = Files.newOutputStream(filePath)) {
        workbook.write(fo);
      }
      sf_logger.info("wrote {}", filePath.toAbsolutePath());
    }
  }

  public Set<String> getNoGeneFoundSet() {
    return noGeneFoundSet;
  }

  public Map<String,String> getLackingDataMap() {
    return lackingDataMap;
  }
}
