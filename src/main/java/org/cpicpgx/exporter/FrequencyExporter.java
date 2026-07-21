package org.cpicpgx.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.ActivityScoreComparator;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.workbook.FrequencyWorkbook;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sawano.java.text.AlphanumericComparator;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Exports a frequency Excel sheet for every gene in the database that has frequency data.
 *
 * @author Ryan Whaley
 */
public class FrequencyExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    FrequencyExporter exporter = new FrequencyExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting frequencies", ex);
    }
  }

  public FileType getFileType() {
    return FileType.FREQUENCY;
  }

  @Override
  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection()) {
      try (
          FrequencyDbHarness dbHarness = new FrequencyDbHarness();
          PreparedStatement stmt = conn.prepareStatement(
              "select distinct a.name, a.id, a.inferredfrequency from allele_frequency f join allele a on f.alleleid = a.id where a.genesymbol=? and a.name != 'Reference' order by 1");
          PreparedStatement popsStmt = conn.prepareStatement(
              "select distinct coalesce(p2.pmid, p2.url, p2.pmcid, p2.doi), p.ethnicity, p.population, p.populationinfo, p.subjecttype, p2.authors, p2.year, p.id, p.subjectcount\n" +
              "from allele_frequency f join population p on f.population = p.id join allele a on f.alleleid = a.id\n" +
              "left join publication p2 on p.publicationId=p2.id\n" +
              "where a.genesymbol=? and p.ethnicity=? order by p.ethnicity, p2.year, p2.authors, p.population");
          PreparedStatement methodsStmt = conn.prepareStatement(
              "select frequencyMethods from gene where symbol=?"
          );
          PreparedStatement geneStmt = conn.prepareStatement("""
            select distinct genesymbol, g.lookupmethod, g.chr, g.includephenotypefrequencies, g.includediplotypefrequencies
            from allele a join gene g on a.genesymbol=g.symbol
            where frequency is not null order by genesymbol""");
          ResultSet geneResults = geneStmt.executeQuery();
          PreparedStatement refAlleleStmt = conn.prepareStatement(
              "select name from allele where allele.inferredfrequency is true and genesymbol=?")
      ) {
        // gene loop
        while (geneResults.next()) {
          String geneSymbol = geneResults.getString(1);
          LookupMethod lookupMethod = LookupMethod.valueOf(geneResults.getString(2));
          String chr = geneResults.getString(3);
          boolean includePheno = geneResults.getBoolean(4);
          boolean includeDiplo = geneResults.getBoolean(5);
          FrequencyWorkbook workbook = new FrequencyWorkbook(geneSymbol, lookupMethod);

          // look up the ref allele name
          String refAlleleName = null;
          refAlleleStmt.setString(1, geneSymbol);
          ResultSet rsRefAllele = refAlleleStmt.executeQuery();
          if (rsRefAllele.next()) {
            refAlleleName = rsRefAllele.getString(1);
          }

          // start the Allele Frequency sheet
          List<String> ethnicities = dbHarness.getAlleleEthnicities(geneSymbol);
          Set<String> alleleNames = dbHarness.getAllelesWithFrequencies(geneSymbol);
          if (alleleNames.isEmpty()) {
            sf_logger.warn("No alleles found for " + geneSymbol);
            continue;
          }

          // load every allele's per-ethnicity frequencies for this gene in one round trip instead of
          // querying once per (ethnicity, allele) pair
          Map<String, Map<String, BigDecimal>> alleleFrequencyMap = dbHarness.getAlleleFrequencyMap(geneSymbol);

          if (!alleleNames.isEmpty()) {
            workbook.writeAlleleFrequencyHeader(ethnicities);

            // infer reference allele values based on other alleles
            if (refAlleleName != null && !alleleNames.contains(refAlleleName)) {
              BigDecimal[] frequencies = new BigDecimal[ethnicities.size()];
              Map<String, BigDecimal> refFreqs = alleleFrequencyMap.getOrDefault(refAlleleName, Collections.emptyMap());
              int idx = 0;
              for (String pop : ethnicities) {
                frequencies[idx++] = refFreqs.get(pop);
              }
              workbook.writeAlleleFrequency(refAlleleName, frequencies);
            }

            for (String allele : alleleNames) {
              BigDecimal[] frequencies = new BigDecimal[ethnicities.size()];
              Map<String, BigDecimal> freqs = alleleFrequencyMap.getOrDefault(allele, Collections.emptyMap());
              int idx = 0;
              for (String pop : ethnicities) {
                frequencies[idx++] = freqs.get(pop);
              }
              if (Arrays.stream(frequencies).anyMatch(Objects::nonNull)) {
                workbook.writeAlleleFrequency(allele, frequencies);
              }
            }
          }
          // end the Allele Frequency sheet


          // start the Diplotype Frequency sheet
          if (!Constants.isSinglePloidy(chr) && includeDiplo) {
            List<String> dipPops = dbHarness.getDiplotypePopulations(geneSymbol);
            if (!dipPops.isEmpty()) {
              workbook.writeDiplotypeFrequencyHeader(dipPops);
              Map<String, HashMap<String, BigDecimal>> diplotypeMap = dbHarness.getDiplotypeData(geneSymbol);

              for (String diplotype : diplotypeMap.keySet()) {
                BigDecimal[] frequencies = new BigDecimal[dipPops.size()];
                Map<String, BigDecimal> popMap = diplotypeMap.get(diplotype);
                if (popMap != null) {
                  int idx = 0;
                  for (String pop : dipPops) {
                    frequencies[idx++] = popMap.get(pop);
                  }
                }
                workbook.writeDiplotypeFrequency(diplotype, frequencies);
              }
            }
          }
          // end the Diplotype Frequency sheet


          // start the Phenotype Frequency sheet
          if (includePheno) {
            List<String> phenoPops = dbHarness.getDiplotypePopulations(geneSymbol);
            if (!phenoPops.isEmpty()) {
              workbook.writePhenotypeFrequencyHeader(phenoPops);
              Map<String, HashMap<String, BigDecimal>> phenotypeMap = dbHarness.getPhenotypeData(geneSymbol, lookupMethod);

              phenotypeMap.forEach((phenotype, popMap) -> {
                BigDecimal[] frequencies = new BigDecimal[phenoPops.size()];
                int idx = 0;
                for (String pop : phenoPops) {
                  frequencies[idx++] = popMap.get(pop);
                }
                workbook.writePhenotypeFrequency(phenotype, frequencies);
              });
            }
          }
          // end the Phenotype Frequency sheet


          Map<String, Integer> alleles = new TreeMap<>(HaplotypeNameComparator.getComparator());
          String refAllele = "";
          stmt.setString(1, geneSymbol);
          try (ResultSet r = stmt.executeQuery()) {
            while (r.next()) {
              String alleleName    = r.getString(1);
              Integer alleleId     = r.getInt(2);
              boolean inferredFreq = r.getBoolean(3);

              // Curators have decided to explicitly skip UGT1A1*1 from frequency due to 1) there are many alleles for
              // UGT1A1 2) we only catalogue a small subset 3) those alleles are single position alleles. The reference
              // allele cannot be simply inferred
              if (geneSymbol.equals("UGT1A1") && alleleName.equals("*1")) {
                continue;
              }

              alleles.put(alleleName, alleleId);
              if (inferredFreq) {
                refAllele = alleleName;
              }
            }
          }


          // We are not guaranteed to have allele_frequency data so skip the References sheet if none
          ethnicities = dbHarness.getEthnicities(geneSymbol);
          if (!ethnicities.isEmpty()) {
            // write the header row
            workbook.writeReferenceHeader(alleles.keySet());

            // load all allele_frequency rows and population summary rows for this gene in one round trip
            // each, instead of querying once per (population, allele) / (ethnicity, allele) pair
            Map<Integer, Map<Integer, String>> alleleFrequencyRows = dbHarness.getAlleleFrequencyRows(geneSymbol);
            Map<String, Map<String, BigDecimal[]>> populationSummary = dbHarness.getPopulationSummary(geneSymbol);

            // population loop (rows)
            for (String ethnicity : ethnicities) {
              popsStmt.setString(1, geneSymbol);
              popsStmt.setString(2, ethnicity);

              workbook.writeEthnicityHeader(ethnicity, alleles.size());

              try (ResultSet r = popsStmt.executeQuery()) {
                while (r.next()) {

                  Array authorArray = r.getArray(6);
                  int popId = r.getInt(8);
                  String[] authors = null;
                  if (authorArray != null) {
                    authors = (String[]) authorArray.getArray();
                  }

                  Map<Integer, String> frequenciesForPopulation = alleleFrequencyRows.getOrDefault(popId, Collections.emptyMap());

                  // allele loop (columns after standard)
                  String[] frequencies = new String[alleles.size()];
                  int i = 0;
                  for (String alleleName : alleles.keySet()) {
                    Integer alleleId = alleles.get(alleleName);
                    frequencies[i] = frequenciesForPopulation.get(alleleId);
                    i += 1;
                  }

                  workbook.writePopulation(
                          authors,
                          r.getInt(7),
                          r.getString(1),
                          r.getString(2),
                          r.getString(3),
                          r.getString(4),
                          r.getString(5),
                          r.getInt(9),
                          frequencies);
                }
              }

              BigDecimal refAlleleFrequency = Optional.ofNullable(
                      alleleFrequencyMap.getOrDefault(refAlleleName, Collections.emptyMap()).get(ethnicity))
                      .orElse(BigDecimal.ZERO);

              workbook.startPopulationSummary();
              for (String allele : alleles.keySet()) {
                BigDecimal[] summary = populationSummary.getOrDefault(allele, Collections.emptyMap()).get(ethnicity);
                if (summary != null) {
                  workbook.writePopulationSummary(summary[0], summary[1], summary[2]);
                } else if (allele.equals(refAllele)) {
                  workbook.writeReferencePopulationSummary(refAlleleFrequency);
                } else {
                  workbook.writeEmptyPopulationSummary();
                }
              }
            }
          }

          // writing the methods for this gene
          methodsStmt.setString(1, geneSymbol);
          String methods;
          try (ResultSet grs = methodsStmt.executeQuery()) {
            if (grs.next()) {
              methods = grs.getString(1);
            } else {
              throw new RuntimeException("No methods text found in DB");
            }
          }
          workbook.writeMethods(methods);

          // writing the notes
          workbook.writeNotes(queryNotes(conn, geneSymbol, FileType.FREQUENCY));

          // writing the change log
          workbook.writeChangeLog(queryChangeLog(conn, geneSymbol, getFileType()));

          writeWorkbook(workbook);
          addFileExportHistory(workbook.getFilename(), new String[]{geneSymbol});
        }
      }
      handleFileUpload();
    }
  }

  private static class FrequencyDbHarness extends DbHarness {
    final Gson gson = new Gson();
    final Type bigDecimalMapType = new TypeToken<HashMap<String, BigDecimal>>(){}.getType();
    PreparedStatement ethnicitiesStmt;
    PreparedStatement diplotypePopStmt;
    PreparedStatement diplotypeDataStmt;
    PreparedStatement phenotypePopStmt;
    PreparedStatement phenotypeDataStmt;
    PreparedStatement activityDataStmt;
    PreparedStatement alleleNameStmt;
    PreparedStatement alleleFrequencyMapStmt;
    PreparedStatement alleleFrequencyRowsStmt;
    PreparedStatement populationSummaryStmt;
    PreparedStatement alleleEthnicityStmt;

    FrequencyDbHarness() throws SQLException {
      super(FileType.FREQUENCY);

      //language=PostgreSQL
      ethnicitiesStmt = prepare("select distinct p.ethnicity from allele_frequency f join allele a on a.id = f.alleleid join population p on f.population = p.id where a.genesymbol=? order by 1");
      alleleEthnicityStmt = prepare("select distinct f from allele a, jsonb_object_keys(frequency) f where frequency is not null and genesymbol=? order by 1");
      //language=PostgreSQL
      diplotypePopStmt = prepare("select distinct jsonb_object_keys(grd.frequency) from gene_result r join gene_result_lookup grl on r.id = grl.phenotypeid join gene_result_diplotype grd on grl.id = grd.functionphenotypeid where r.genesymbol=? and grd.frequency is not null order by 1");
      //language=PostgreSQL
      diplotypeDataStmt = prepare("select grd.diplotype, grd.frequency from gene_result r join gene_result_lookup grl on r.id = grl.phenotypeid join gene_result_diplotype grd on grl.id = grd.functionphenotypeid where r.genesymbol=?");
      //language=PostgreSQL
      phenotypePopStmt = prepare("select distinct jsonb_object_keys(frequency) from gene_result where genesymbol=? and frequency is not null order by 1");
      //language=PostgreSQL
      phenotypeDataStmt = prepare("select result,frequency from gene_result where genesymbol=? and frequency is not null order by result desc");
      //language=PostgreSQL
      activityDataStmt = prepare("select activityscore,frequency from gene_result where genesymbol=? and frequency is not null");
      //language=PostgreSQL
      alleleNameStmt = prepare("select distinct name from allele where genesymbol=? and frequency is not null");
      //language=PostgreSQL
      alleleFrequencyMapStmt = prepare("select name, frequency from allele where genesymbol=? and frequency is not null");
      //language=PostgreSQL
      alleleFrequencyRowsStmt = prepare("select f.population, f.alleleid, f.label, f.frequency from allele_frequency f join allele a on f.alleleid=a.id where a.genesymbol=?");
      //language=PostgreSQL
      populationSummaryStmt = prepare("select name, population_group, freq_min, freq_weighted_avg, freq_max from population_frequency_view where genesymbol=?");
    }

    Set<String> getAllelesWithFrequencies(String gene) throws SQLException {
      Set<String> alleleNames = new TreeSet<>(new AlphanumericComparator());
      alleleNameStmt.setString(1, gene);
      try (ResultSet rs = alleleNameStmt.executeQuery()) {
        while (rs.next()) {
          alleleNames.add(rs.getString(1));
        }
      }
      return alleleNames;
    }

    /**
     * Loads every allele's per-ethnicity frequency for a gene in a single round trip, keyed by allele name then
     * ethnicity. Replaces querying the allele's {@code frequency} JSONB column once per (ethnicity, allele) pair.
     */
    Map<String, Map<String, BigDecimal>> getAlleleFrequencyMap(String gene) throws SQLException {
      Map<String, Map<String, BigDecimal>> result = new HashMap<>();
      alleleFrequencyMapStmt.setString(1, gene);
      try (ResultSet rs = alleleFrequencyMapStmt.executeQuery()) {
        while (rs.next()) {
          result.put(rs.getString(1), gson.fromJson(rs.getString(2), bigDecimalMapType));
        }
      }
      return result;
    }

    /**
     * Loads every allele_frequency row for a gene in a single round trip, keyed by population ID then allele ID.
     * Replaces querying {@code allele_frequency} once per (population, allele) pair.
     */
    Map<Integer, Map<Integer, String>> getAlleleFrequencyRows(String gene) throws SQLException {
      Map<Integer, Map<Integer, String>> result = new HashMap<>();
      alleleFrequencyRowsStmt.setString(1, gene);
      try (ResultSet rs = alleleFrequencyRowsStmt.executeQuery()) {
        while (rs.next()) {
          int popId = rs.getInt(1);
          int alleleId = rs.getInt(2);
          String label = rs.getString(3);
          BigDecimal freq = rs.getBigDecimal(4);
          String value = (freq != null && freq.compareTo(BigDecimal.ZERO) != 0) ? freq.toString() : label;
          result.computeIfAbsent(popId, k -> new HashMap<>()).put(alleleId, value);
        }
      }
      return result;
    }

    /**
     * Loads every population_frequency_view row for a gene in a single round trip, keyed by allele name then
     * ethnicity, as {@code [freq_min, freq_weighted_avg, freq_max]}. Replaces querying that view once per
     * (ethnicity, allele) pair.
     */
    Map<String, Map<String, BigDecimal[]>> getPopulationSummary(String gene) throws SQLException {
      Map<String, Map<String, BigDecimal[]>> result = new HashMap<>();
      populationSummaryStmt.setString(1, gene);
      try (ResultSet rs = populationSummaryStmt.executeQuery()) {
        while (rs.next()) {
          String allele = rs.getString(1);
          String ethnicity = rs.getString(2);
          BigDecimal[] values = new BigDecimal[]{rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5)};
          result.computeIfAbsent(allele, k -> new HashMap<>()).put(ethnicity, values);
        }
      }
      return result;
    }

    List<String> getEthnicities(String gene) throws SQLException {
      List<String> result = new ArrayList<>();
      if (StringUtils.isNotBlank(gene)) {
        this.ethnicitiesStmt.clearParameters();
        this.ethnicitiesStmt.setString(1, gene);
        try (ResultSet rs = this.ethnicitiesStmt.executeQuery()) {
          while (rs.next()) {
            result.add(rs.getString(1));
          }
        }
      }
      return result;
    }

    List<String> getAlleleEthnicities(String gene) throws SQLException {
      List<String> result = new ArrayList<>();
      if (StringUtils.isNotBlank(gene)) {
        this.alleleEthnicityStmt.clearParameters();
        this.alleleEthnicityStmt.setString(1, gene);
        try (ResultSet rs = this.alleleEthnicityStmt.executeQuery()) {
          while (rs.next()) {
            result.add(rs.getString(1));
          }
        }
      }
      return result;
    }

    List<String> getDiplotypePopulations(String gene) throws SQLException {
      List<String> result = new ArrayList<>();
      if (StringUtils.isNotBlank(gene)) {
        this.diplotypePopStmt.clearParameters();
        this.diplotypePopStmt.setString(1, gene);
        try (ResultSet rs = this.diplotypePopStmt.executeQuery()) {
          while (rs.next()) {
            result.add(rs.getString(1));
          }
        }
      }
      return result;
    }

    Map<String, HashMap<String,BigDecimal>> getDiplotypeData(String gene) throws SQLException {
      Map<String, HashMap<String,BigDecimal>> result = new TreeMap<>(HaplotypeNameComparator.getComparator());
      if (StringUtils.isNotBlank(gene)) {
        this.diplotypeDataStmt.clearParameters();
        this.diplotypeDataStmt.setString(1, gene);
        try (ResultSet rs = this.diplotypeDataStmt.executeQuery()) {
          while (rs.next()) {
            result.put(rs.getString(1), gson.fromJson(rs.getString(2), bigDecimalMapType));
          }
        }
      }
      return result;
    }

    Map<String, HashMap<String,BigDecimal>> getPhenotypeData(String gene, LookupMethod lookupMethod) throws SQLException {
      Comparator<String> comparator = lookupMethod == LookupMethod.ACTIVITY_SCORE ? ActivityScoreComparator.getComparator() : Comparator.reverseOrder();
      PreparedStatement query = lookupMethod == LookupMethod.ACTIVITY_SCORE ? this.activityDataStmt : phenotypeDataStmt;
      Map<String, HashMap<String,BigDecimal>> result = new TreeMap<>(comparator);
      if (StringUtils.isNotBlank(gene)) {
        query.clearParameters();
        query.setString(1, gene);
        try (ResultSet rs = query.executeQuery()) {
          while (rs.next()) {
            result.put(rs.getString(1), gson.fromJson(rs.getString(2), bigDecimalMapType));
          }
        }
      }
      return result;
    }
  }
}
