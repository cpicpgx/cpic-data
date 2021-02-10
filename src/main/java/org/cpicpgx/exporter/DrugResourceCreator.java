package org.cpicpgx.exporter;

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
import java.nio.file.Paths;
import java.util.*;

import static org.cpicpgx.util.HttpUtils.apiRequest;
import static org.cpicpgx.util.HttpUtils.buildPgkbUrl;

/**
 * This class will create Drug Resource files for drugs based on their names. This will query the PharmGKB API and match
 * drugs to their primary name and then write the found data into a Drug Resource file. The file should be processed by
 * the {@link org.cpicpgx.importer.DrugImporter} class to add the drug to the DB.
 */
public class DrugResourceCreator {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("n", true,"1 or more drug names separated by semi-colons (;)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      String drugNames = cli.getOptionValue("n");

      DrugResourceCreator drugResourceCreator = new DrugResourceCreator();
      for (String name : drugNames.split(";")) {
        drugResourceCreator.create(name);
      }

      if (drugResourceCreator.getNoDrugFoundSet().size() > 0) {
        sf_logger.warn("Import these drugs to PharmGKB: " + String.join("; ", drugResourceCreator.getNoDrugFoundSet()));
      }

      if (drugResourceCreator.getLackingDataMap().size() > 0) {
        sf_logger.warn("Fill in more PharmGKB data for:");
        for (String name : drugResourceCreator.getLackingDataMap().keySet()) {
          sf_logger.warn("{} = {}", name, drugResourceCreator.getLackingDataMap().get(name));
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

  private final Set<String> noDrugFoundSet = new TreeSet<>();
  private final Map<String,String> lackingDataMap = new TreeMap<>();
  private final OkHttpClient f_httpClient;
  private final Gson f_gson;

  /**
   *
   */
  public DrugResourceCreator() {
    f_httpClient = new OkHttpClient().newBuilder()
        .build();
    f_gson = new Gson();
  }

  public void create(String rawName) throws Exception {
    Thread.sleep(HttpUtils.API_WAIT_TIME);
    String name = StringUtils.strip(rawName);

    sf_logger.info("add {}", name);

    String response;
    try {
      response = apiRequest(f_httpClient, buildPgkbUrl("data/chemical", "view", "max", "name", name));
    } catch (Exception ex) {
      sf_logger.warn("No data found for {}: {}", name, ex.getMessage());
      if (ex instanceof NotFoundException) {
        noDrugFoundSet.add(name);
      }
      return;
    }

    if (StringUtils.isBlank(response)) {
      sf_logger.warn("No data found for {}", name);
      return;
    }

    JsonObject responseJson = f_gson.fromJson(response, JsonObject.class);
    JsonArray dataArray = responseJson.getAsJsonArray("data");
    if (dataArray.size() > 1) {
      sf_logger.warn("More than one drug found for {}", name);
      return;
    }

    JsonObject drugJson = dataArray.get(0).getAsJsonObject();

    String pharmgkbId = drugJson.get("id").getAsString();
    String rxNormId = null;
    String drugBankId = null;
    List<String> atcIds = new ArrayList<>();

    JsonArray termArray = drugJson.getAsJsonArray("terms");
    for (JsonElement termElement : termArray) {
      String resource = termElement.getAsJsonObject().get("resource").getAsString();
      if (resource.equals("RxNorm")) {
        rxNormId = termElement.getAsJsonObject().get("termId").getAsString();
      }
      else if (resource.equals("Anatomical Therapeutic Chemical Classification")) {
        atcIds.add(termElement.getAsJsonObject().get("termId").getAsString());
      }
    }

    JsonArray crossReferenceArray = drugJson.getAsJsonArray("crossReferences");
    for (JsonElement crossReferenceElement : crossReferenceArray) {
      String resource = crossReferenceElement.getAsJsonObject().get("resource").getAsString();
      if (resource.equals("DrugBank")) {
        drugBankId = crossReferenceElement.getAsJsonObject().get("resourceId").getAsString();
      }
    }

    sf_logger.info("PharmGKB ID {} = RxNorm {}, DrugBank {}, ATC {}", pharmgkbId, rxNormId, drugBankId, String.join(";", atcIds));

    if (rxNormId == null && drugBankId == null && atcIds.size() == 0) {
      lackingDataMap.put(name, pharmgkbId);
    } else {
      DrugResourceWorkbook workbook = new DrugResourceWorkbook(name);
      workbook.writeMapping(rxNormId, drugBankId, atcIds.toArray(new String[]{}), pharmgkbId);
      workbook.getSheets().forEach(SheetWrapper::autosizeColumns);
      try (OutputStream fo = Files.newOutputStream(Paths.get("out", workbook.getFilename()))) {
        workbook.write(fo);
      }
    }
  }

  public Set<String> getNoDrugFoundSet() {
    return noDrugFoundSet;
  }

  public Map<String,String> getLackingDataMap() {
    return lackingDataMap;
  }
}
