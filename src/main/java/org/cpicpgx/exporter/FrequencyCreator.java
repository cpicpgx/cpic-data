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
import static org.cpicpgx.util.HttpUtils.buildClinpgxUrl;

/**
 * This class will create a new frequency workbook for a given gene using the frequency data in ClinPGx.
 *
 * <p><strong>NOTE:</strong> This only works for "named variant" genes like DPYD. Within those genes, it will only
 * output frequencies for the single-position alleles (not Haplotypes).</p>
 */
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
    sf_logger.info("Writing new frequency data for: {}", f_gene);
    loadAlleles();
    f_alleleRsidMap.keySet().forEach(this::loadFrequency);
  }

  private void loadAlleles() {
    sf_logger.debug("Load data from DB for {}", f_gene);
    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement stmt = conn.prepareStatement("""
select d.id, alv.locationid, d.name, l.dbsnpid, alv.variantallele
from
    allele_definition d
    join allele_location_value alv on d.id=alv.alleledefinitionid
    join sequence_location l on alv.locationid=l.id
where
    d.genesymbol=?
  and 1=(select count(*) from allele_location_value v where v.alleledefinitionid=d.id)
  and l.dbsnpid is not null
""");
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

      stmt = conn.prepareStatement("select name from allele_definition where genesymbol=?");
      stmt.setString(1, f_gene);
      try (ResultSet results = stmt.executeQuery()) {
        while (results.next()) {
          String alleleName = results.getString(1);
          f_allAlleles.add(alleleName);
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
      sf_logger.debug("Requesting frequencies from ClinPGx for: {}", rsid);
      response = apiRequest(f_httpClient, buildClinpgxUrl("report/variantFrequency", "fp", rsid, "source", "gnomadV4"));
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

    JsonArray allFrequencies = jsendObject.getAsJsonArray("data");

    for (JsonElement frequency : allFrequencies) {
      JsonObject frequencyObject = frequency.getAsJsonObject();
      String populationString = frequencyObject.get("population").getAsString();
      GnomadPopulation population = GnomadPopulation.valueOf(populationString);

      BigDecimal freq = BigDecimal.ZERO;

      String inBase = frequencyObject.get("alleleText").getAsString();
      if (f_alleleMap.get(alleleName).equals(inBase)) {
        JsonElement frequencyElement = frequencyObject.get("frequency");
        if (frequencyElement != null && !frequencyElement.isJsonNull() &&
                !(frequencyElement.isJsonPrimitive() && frequencyElement.getAsJsonPrimitive().isString())) {
          freq = frequencyElement.getAsBigDecimal();
        }
      }
      if (frequencyObject.has("totalAlleles")) {
        alleleDistribution.addSize(population, frequencyObject.get("totalAlleles").getAsInt());
      }

      alleleDistribution.set(population, freq);
    }
    f_alleleDistributions.add(alleleDistribution);
  }

  public void write(Path outputDir) throws IOException {
    FrequencyWorkbook workbook = new FrequencyWorkbook(f_gene, m_lookupMethod);
    List<String> groupNames = GnomadPopulation.getCpgxGroups();

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
    for (String groupName : GnomadPopulation.getCpgxGroups()) {
      workbook.writeEthnicityHeader(groupName, f_allAlleles.size());
      for (GnomadPopulation population : GnomadPopulation.getGnomadsForCpgx(groupName)) {
        int i = 0;
        for (String alleleName : f_allAlleles) {
          final int k = i;
          f_alleleDistributions.stream()
                  .filter(d -> d.getAlleleName().equals(alleleName) && d.getSize(population) > 0)
                  .findFirst()
              .ifPresent(
                  (d) -> {
                    List<String> freqsForAllelesList = new ArrayList<>(f_allAlleles.size());
                    for (int j = 0; j < f_allAlleles.size(); j++) {
                      if (k == j) {
                        freqsForAllelesList.add(d.getFreqAsString(population));
                      } else {
                        freqsForAllelesList.add("");
                      }
                    }

                    workbook.writePopulation(
                            dummyAuthors,
                            LocalDate.now().getYear(),
                            "",
                            population.getName(),
                            population.getVersionedName(),
                            "",
                            "",
                            d.getSize(population),
                            freqsForAllelesList
                    );
                  }
              );
          i += 1;
        }
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
    // finish Reference sheet writing

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
