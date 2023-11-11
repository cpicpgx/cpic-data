package org.cpicpgx.exporter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.AlleleDistribution;
import org.cpicpgx.model.GnomadPopulation;
import org.cpicpgx.util.HttpUtils;
import org.cpicpgx.workbook.FrequencyWorkbook;
import org.cpicpgx.workbook.SheetWrapper;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.cpicpgx.util.HttpUtils.apiRequest;
import static org.cpicpgx.util.HttpUtils.buildPgkbUrl;

public class FrequencyCreator {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("g", true,"gene to make frequency data for");
      options.addOption("d", true,"directory to write output to, optional ('out' default)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      String geneSymbol = StringUtils.trimToNull(cli.getOptionValue("g"));
      if (geneSymbol == null) {
        throw new IllegalArgumentException("No gene specified");
      }

      FrequencyCreator frequencyCreator = new FrequencyCreator(geneSymbol);
      Path dir;
      if (cli.hasOption("d")) {
        dir = Paths.get(cli.getOptionValue("d"));
        if (!Files.exists(dir)) {
          Files.createDirectories(dir);
        }
      } else {
        dir = Paths.get("out");
      }
      frequencyCreator.write(dir);

      System.exit(0);
    } catch (IOException|ParseException e) {
      sf_logger.error("Failed to create frequencies", e);
      System.exit(1);
    }
  }

  private final OkHttpClient f_httpClient;
  private final Gson f_gson;
  private final String f_gene;
  private final Map<String, String> f_alleleRsidMap = new TreeMap<>(HaplotypeNameComparator.getComparator());
  private final Map<String, String> f_alleleMap = new HashMap<>();
  private final SortedSet<String> f_allAlleles = new TreeSet<>(HaplotypeNameComparator.getComparator());
  private LookupMethod m_lookupMethod = LookupMethod.PHENOTYPE;
  private final List<AlleleDistribution> f_alleleDistributions = new ArrayList<>();

  public FrequencyCreator(String gene) {
    f_httpClient = new OkHttpClient().newBuilder().build();
    f_gson = new Gson();
    f_gene = gene;
    sf_logger.info("Writing new frequency data for: " + f_gene);
    loadAlleles();
    f_alleleRsidMap.keySet().forEach(this::loadFrequency);
  }

  private void loadAlleles() {
    sf_logger.debug("Load data from DB for {}", f_gene);
    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement stmt = conn.prepareStatement(
          "with x as (\n" +
              "    select alv.alleledefinitionid, alv.locationid, ad.name, alv.variantallele from allele_location_value alv\n" +
              "        join allele_definition ad on alv.alleledefinitionid = ad.id\n" +
              "    where ad.matchesreferencesequence is false and ad.genesymbol=? and alv.variantallele !~ '[KMRSWY]'\n" +
              ")\n" +
              "select x1.alleledefinitionid, x1.locationid, x1.name, l.dbsnpid, x1.variantallele from x x1 join sequence_location l on (x1.locationid=l.id)\n" +
              "where not exists (select 1 from x x2 where x1.alleledefinitionid!=x2.alleledefinitionid and x1.locationid =x2.locationid)\n" +
              "  and not exists (select 1 from x x2 where x1.alleledefinitionid =x2.alleledefinitionid and x1.locationid!=x2.locationid)\n" +
              "  and l.dbsnpid is not null");
      stmt.setString(1, f_gene);

      try (ResultSet results = stmt.executeQuery()) {
        while (results.next()) {
          String alleleName = results.getString(3);
          String rsid = results.getString(4);
          String variant = results.getString(5);

          sf_logger.debug("{} >>> {}", alleleName, rsid);

          f_alleleRsidMap.put(alleleName, rsid);
          f_alleleMap.put(alleleName, variant);
        }
      }

      stmt = conn.prepareStatement("select name, matchesreferencesequence from allele_definition where genesymbol=?");
      stmt.setString(1, f_gene);
      try (ResultSet results = stmt.executeQuery()) {
        while (results.next()) {
          String alleleName = results.getString(1);
          boolean isReference = results.getBoolean(2);
          if (isReference) {
            f_allAlleles.add(alleleName);
          } else {
            f_allAlleles.add(alleleName);
          }
        }
      }

      stmt = conn.prepareStatement("select lookupmethod from gene where symbol=?");
      stmt.setString(1, f_gene);

      try (ResultSet results = stmt.executeQuery()) {
        while (results.next()) {
          m_lookupMethod = LookupMethod.valueOf(results.getString(1));
        }
      }
    } catch (SQLException e) {
      sf_logger.error("Error loading alleles", e);
    }
  }

  private void loadFrequency(String alleleName) {
    AlleleDistribution alleleDistribution = new AlleleDistribution(f_gene, alleleName);

    String rsid = f_alleleRsidMap.get(alleleName);

    String response = null;
    try {
      Thread.sleep(HttpUtils.API_WAIT_TIME);
      sf_logger.debug("Requesting frequencies from PharmGKB for: {}", rsid);
      response = apiRequest(f_httpClient, buildPgkbUrl("report/variantFrequency", "fp", rsid, "source", "gnomadGenome"));
    } catch (NotFoundException e) {
      // safe to ignore, just means no frequency data available
    } catch (Exception e) {
      throw new RuntimeException("Could not parse frequency response", e);
    }

    if (StringUtils.isBlank(response)) {
      return;
    }

    JsonObject jsendObject = f_gson.fromJson(response, JsonObject.class);
    if (!jsendObject.has("data") || jsendObject.get("data").isJsonNull()) {
      // no data to parse
      return;
    }

    JsonObject frequencyObject = jsendObject.getAsJsonObject("data");
    JsonArray populationArray = frequencyObject.getAsJsonArray("populations");

    for (JsonElement populationElement : populationArray) {
      JsonObject populationObject = populationElement.getAsJsonObject();
      String populationString = populationObject.get("population").getAsString();
      GnomadPopulation population = GnomadPopulation.valueOf(populationString);

      BigDecimal freq = BigDecimal.ZERO;
      for (JsonElement alleleElement : populationObject.getAsJsonArray("bases")) {
        JsonObject alleleObject = alleleElement.getAsJsonObject();
        if (f_alleleMap.get(alleleName).equals(alleleObject.get("base").getAsString())) {
          freq = alleleObject.get("freq").getAsBigDecimal();
        }
        if (alleleObject.has("size")) {
          alleleDistribution.addSize(population, alleleObject.get("size").getAsInt());
        }
      }

      alleleDistribution.set(population, freq);
    }
    f_alleleDistributions.add(alleleDistribution);
  }

  public void write(Path outputDir) throws IOException {
    FrequencyWorkbook workbook = new FrequencyWorkbook(f_gene, m_lookupMethod);
    List<String> groupNames = GnomadPopulation.getPgkbGroups();

    // start allele sheet writing
    workbook.writeAlleleFrequencyHeader(groupNames);
    for (String alleleName : f_allAlleles) {
      f_alleleDistributions.stream()
          .filter((d) -> d.getAlleleName().equals(alleleName))
          .findFirst()
          .ifPresentOrElse(
              (d) -> {
                BigDecimal[] popFreq = new BigDecimal[groupNames.size()];
                for (int i = 0; i < groupNames.size(); i++) {
                  popFreq[i] = d.getAvgFreqForGroup(groupNames.get(i));
                }
                workbook.writeAlleleFrequency(alleleName, popFreq);
              },
              () -> {
                BigDecimal[] popFreq = new BigDecimal[groupNames.size()];
                workbook.writeAlleleFrequency(alleleName, popFreq);
              }
          );
    }
    // finish allele sheet writing

    workbook.writeDiplotypeFrequencyHeader(groupNames);
    workbook.writePhenotypeFrequencyHeader(groupNames);

    // start "Reference" sheet writing
    workbook.writeReferenceHeader(f_allAlleles);
    String[] dummyAuthors = new String[]{""};
    for (String groupName : GnomadPopulation.getPgkbGroups()) {
      workbook.writeEthnicityHeader(groupName, f_allAlleles.size());
      for (GnomadPopulation population : GnomadPopulation.getGnomadsForPgkb(groupName)) {
        List<String> freqsForAllelesList = new ArrayList<>();
        for (String alleleName : f_allAlleles) {
          f_alleleDistributions.stream().filter(d -> d.getAlleleName().equals(alleleName)).findFirst()
              .ifPresentOrElse(
                  (d) -> freqsForAllelesList.add(d.getFreqAsString(population)),
                  () -> freqsForAllelesList.add("")
              );
        }
        int subjectCount = f_alleleDistributions.get(0).getSize(population);

        workbook.writePopulation(
            dummyAuthors,
            LocalDate.now().getYear(),
            "",
            population.getName(),
            population.getVersionedName(),
            "",
            "",
            subjectCount,
            freqsForAllelesList
        );

      }
      workbook.startPopulationSummary();
      for (String alleleName : f_allAlleles) {
        f_alleleDistributions.stream().filter(d -> d.getAlleleName().equals(alleleName)).findFirst()
            .ifPresentOrElse(
                (d) -> {
                  BigDecimal avg = d.getAvgFreqForGroup(groupName);
                  BigDecimal max = d.getMaxFreqForGroup(groupName);
                  BigDecimal min = d.getMinFreqForGroup(groupName);
                  workbook.writePopulationSummary(min, avg, max);
                },
                () -> workbook.writePopulationSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            );
      }
    }
    // finish "Reference sheet writing

    workbook.writeMethods("");
    workbook.writeNotes(ImmutableList.of(""));
    workbook.writeChangeLog(ImmutableList.of(new Object[]{new Date(), "File created"}));


    // write to disk
    workbook.getSheets().forEach(SheetWrapper::autosizeColumns);
    Path filePath = outputDir.resolve(workbook.getFilename());
    try (OutputStream out = Files.newOutputStream(filePath)) {
      workbook.write(out);
    }
    sf_logger.info("Wrote {}", filePath.toAbsolutePath());
  }
}
