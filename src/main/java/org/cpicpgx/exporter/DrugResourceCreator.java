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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
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
      options.addOption("o", true,"path to output directory");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      String drugNames = cli.getOptionValue("n");

      DrugResourceCreator drugResourceCreator = new DrugResourceCreator(cli.getOptionValue("o", "out"));
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
  private final String f_outputPath;

  /**
   *
   */
  public DrugResourceCreator(String outputPath) {
    f_httpClient = new OkHttpClient().newBuilder()
        .build();
    f_gson = new Gson();
    f_outputPath = outputPath;
  }

  public Path create(String rawName) {
    String name = StringUtils.strip(rawName);

    String response = null;
    try {
      Thread.sleep(HttpUtils.API_WAIT_TIME);
      response = apiRequest(f_httpClient, buildPgkbUrl("data/chemical", "view", "max", "name", name.toLowerCase(Locale.ROOT)));
    } catch (NotFoundException ex) {
      // safe to ignore 404's
    } catch (Exception ex) {
      throw new RuntimeException("Problem getting drug data", ex);
    }

    String pharmgkbId = "";
    String rxNormId = "";
    String drugBankId = "";
    List<String> atcIds = new ArrayList<>();

    if (StringUtils.isNotBlank(response)) {
      JsonObject responseJson = f_gson.fromJson(response, JsonObject.class);
      JsonArray dataArray = responseJson.getAsJsonArray("data");
      if (dataArray.size() > 1) {
        throw new RuntimeException("More than one drug found for " + name);
      }
      JsonObject drugJson = dataArray.get(0).getAsJsonObject();

      pharmgkbId = drugJson.get("id").getAsString();

      JsonArray termArray = drugJson.getAsJsonArray("terms");
      for (JsonElement termElement : termArray) {
        String resource = termElement.getAsJsonObject().get("resource").getAsString();
        if (resource.equals("RxNorm")) {
          rxNormId = termElement.getAsJsonObject().get("termId").getAsString();
        } else if (resource.equals("Anatomical Therapeutic Chemical Classification")) {
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
    }

    DrugResourceWorkbook workbook = new DrugResourceWorkbook(name);
    workbook.writeMapping(rxNormId, drugBankId, atcIds.toArray(new String[]{}), pharmgkbId);
    workbook.getSheets().forEach(SheetWrapper::autosizeColumns);
    Path filePath = Paths.get(f_outputPath, workbook.getFilename());
    try (OutputStream fo = Files.newOutputStream(filePath)) {
      workbook.write(fo);
    } catch (IOException ex) {
      throw new RuntimeException("Could not write drug resource file for " + workbook.getFilename(), ex);
    }
    return filePath;
  }

  public Set<String> getNoDrugFoundSet() {
    return noDrugFoundSet;
  }

  public Map<String,String> getLackingDataMap() {
    return lackingDataMap;
  }
}
