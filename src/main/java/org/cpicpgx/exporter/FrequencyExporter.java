package org.cpicpgx.exporter;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.util.Constants;
import org.cpicpgx.workbook.FrequencyWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.ActivityScoreComparator;
import org.cpicpgx.util.DbHarness;
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
  private static final List<String> BLOCKLIST_DIPLO = ImmutableList.of("CACNA1S", "RYR1", "HLA-A", "HLA-B");
  private static final List<String> BLOCKLIST_PHENO = ImmutableList.of("CACNA1S", "RYR1", "HLA-A", "HLA-B", "MT-RNR1", "VKORC1", "CYP4F2");

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
              "select distinct a.name, a.id, ad.matchesreferencesequence from allele_frequency f join allele a on f.alleleid = a.id join allele_definition ad on a.definitionid = ad.id where a.genesymbol=? and a.name != 'Reference' order by 1");
          PreparedStatement popsStmt = conn.prepareStatement(
              "select distinct coalesce(p2.pmid, p2.url, p2.pmcid, p2.doi), p.ethnicity, p.population, p.populationinfo, p.subjecttype, p2.authors, p2.year, p.id, p.subjectcount\n" +
              "from allele_frequency f join population p on f.population = p.id join allele a on f.alleleid = a.id\n" +
              "left join publication p2 on p.publicationId=p2.id\n" +
              "where a.genesymbol=? and p.ethnicity=? order by p.ethnicity, p2.year, p2.authors, p.population");
          PreparedStatement afStmt = conn.prepareStatement(
              "select f.label, f.frequency from allele_frequency f where f.population=? and f.alleleid=?");
          PreparedStatement ethAlleleStmt = conn.prepareStatement(
              "select freq_weighted_avg, freq_max, freq_min from population_frequency_view v where v.name=? and v.population_group=? and v.genesymbol=?"
          );
          PreparedStatement methodsStmt = conn.prepareStatement(
              "select frequencyMethods from gene where symbol=?"
          );
          PreparedStatement geneStmt = conn.prepareStatement(
              "select distinct a.genesymbol, g.lookupmethod, g.chr from allele_frequency f join allele a on a.id = f.alleleid\n" +
                  "    join gene g on a.genesymbol = g.symbol\n" +
                  "order by 1");
          ResultSet geneResults = geneStmt.executeQuery();
          PreparedStatement refAlleleStmt = conn.prepareStatement(
              "select name from allele_definition where matchesreferencesequence is true and genesymbol=? and name != 'Reference'")
      ) {
        // gene loop
        while (geneResults.next()) {
          String geneSymbol = geneResults.getString(1);
          LookupMethod lookupMethod = LookupMethod.valueOf(geneResults.getString(2));
          String chr = geneResults.getString(3);
          FrequencyWorkbook workbook = new FrequencyWorkbook(geneSymbol, lookupMethod);

          // look up the ref allele name
          String refAlleleName = null;
          refAlleleStmt.setString(1, geneSymbol);
          ResultSet rsRefAllele = refAlleleStmt.executeQuery();
          if (rsRefAllele.next()) {
            refAlleleName = rsRefAllele.getString(1);
          }

          // start the Allele Frequency sheet
          List<String> ethnicities = dbHarness.getEthnicities(geneSymbol);
          Set<String> alleleNames = dbHarness.getAllelesWithFrequencies(geneSymbol);
          if (alleleNames.size() == 0) {
            throw new RuntimeException("No alleles found");
          }

          if (ethnicities.size() > 0) {
            workbook.writeAlleleFrequencyHeader(ethnicities);

            // infer reference allele values based on other alleles
            if (refAlleleName != null && !alleleNames.contains(refAlleleName)) {
              BigDecimal[] frequencies = new BigDecimal[ethnicities.size()];
              for (String pop : ethnicities) {
                frequencies[ethnicities.indexOf(pop)] = dbHarness.getFrequency(geneSymbol, refAlleleName, pop);
              }
              workbook.writeAlleleFrequency(refAlleleName, frequencies);
            }

            for (String allele : alleleNames) {
              BigDecimal[] frequencies = new BigDecimal[ethnicities.size()];
              for (String pop : ethnicities) {
                frequencies[ethnicities.indexOf(pop)] = dbHarness.getFrequency(geneSymbol, allele, pop);
              }
              workbook.writeAlleleFrequency(allele, frequencies);
            }
          }
          // end the Allele Frequency sheet


          // start the Diplotype Frequency sheet
          if (!Constants.isSinglePloidy(chr) && !BLOCKLIST_DIPLO.contains(geneSymbol)) {
            List<String> dipPops = dbHarness.getDiplotypePopulations(geneSymbol);
            if (dipPops.size() > 0) {
              workbook.writeDiplotypeFrequencyHeader(dipPops);
              Map<String, HashMap<String, BigDecimal>> diplotypeMap = dbHarness.getDiplotypeData(geneSymbol);

              for (String diplotype : diplotypeMap.keySet()) {
                BigDecimal[] frequencies = new BigDecimal[dipPops.size()];
                Map<String, BigDecimal> popMap = diplotypeMap.get(diplotype);
                if (popMap != null) {
                  for (String pop : dipPops) {
                    int idx = dipPops.indexOf(pop);
                    if (idx > -1) {
                      frequencies[idx] = popMap.get(pop);
                    }
                  }
                }
                workbook.writeDiplotypeFrequency(diplotype, frequencies);
              }
            }
          }
          // end the Diplotype Frequency sheet


          // start the Phenotype Frequency sheet
          if (!BLOCKLIST_PHENO.contains(geneSymbol)) {
            List<String> phenoPops = dbHarness.getDiplotypePopulations(geneSymbol);
            if (phenoPops.size() > 0) {
              workbook.writePhenotypeFrequencyHeader(phenoPops);
              Map<String, HashMap<String, BigDecimal>> phenotypeMap = dbHarness.getPhenotypeData(geneSymbol, lookupMethod);

              phenotypeMap.forEach((phenotype, popMap) -> {
                BigDecimal[] frequencies = new BigDecimal[phenoPops.size()];
                for (String pop : phenoPops) {
                  int idx = phenoPops.indexOf(pop);
                  if (idx > -1) {
                    frequencies[idx] = popMap.get(pop);
                  }
                }
                workbook.writePhenotypeFrequency(phenotype, frequencies);
              });
            }
          }
          // end the Phenotype Frequency sheet


          // write the header row
          Map<String, Integer> alleles = new TreeMap<>(HaplotypeNameComparator.getComparator());
          String refAllele = "";
          stmt.setString(1, geneSymbol);
          try (ResultSet r = stmt.executeQuery()) {
            while (r.next()) {
              alleles.put(r.getString(1), r.getInt(2));
              if (r.getBoolean(3)) {
                refAllele = r.getString(1);
              }
            }
          }
          workbook.writeReferenceHeader(alleles.keySet());
          
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

                // allele loop (columns after standard)
                String[] frequencies = new String[alleles.keySet().size()];
                int i = 0;
                for (String alleleName : alleles.keySet()) {
                  Integer alleleId = alleles.get(alleleName);
                  afStmt.clearParameters();
                  afStmt.setInt(1, popId);
                  afStmt.setInt(2, alleleId);
                  try (ResultSet afrs = afStmt.executeQuery()) {
                    while (afrs.next()) {
                      String label = afrs.getString(1);
                      BigDecimal freq = afrs.getBigDecimal(2);
                      if (freq != null && freq.compareTo(BigDecimal.ZERO) != 0) {
                        frequencies[i] = freq.toString();
                      } else {
                        frequencies[i] = label;
                      }
                    }
                  }
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

            BigDecimal refAlleleFrequency = Optional.ofNullable(dbHarness.getFrequency(geneSymbol, refAlleleName, ethnicity))
                    .orElse(BigDecimal.ZERO);

            workbook.startPopulationSummary();
            for (String allele : alleles.keySet()) {
              BigDecimal refFreq = Optional.ofNullable(dbHarness.getFrequency(geneSymbol, allele, ethnicity))
                      .orElse(BigDecimal.ZERO);

              ethAlleleStmt.setString(1, allele);
              ethAlleleStmt.setString(2, ethnicity);
              ethAlleleStmt.setString(3, geneSymbol);
              try (ResultSet rsEth = ethAlleleStmt.executeQuery()) {
                boolean wroteSummary = false;
                while (rsEth.next()) {
                  workbook.writePopulationSummary(rsEth.getBigDecimal(3), refFreq, rsEth.getBigDecimal(2));
                  wroteSummary = true;
                }
                if (!wroteSummary) {
                  if (allele.equals(refAllele)) {
                    workbook.writeReferencePopulationSummary(refAlleleFrequency);
                  } else {
                    workbook.writeEmptyPopulationSummary();
                  }
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
    PreparedStatement allelePopulationStmt;
    PreparedStatement diplotypePopStmt;
    PreparedStatement diplotypeDataStmt;
    PreparedStatement phenotypePopStmt;
    PreparedStatement phenotypeDataStmt;
    PreparedStatement activityDataStmt;
    PreparedStatement alleleNameStmt;

    FrequencyDbHarness() throws SQLException {
      super(FileType.FREQUENCY);

      //language=PostgreSQL
      ethnicitiesStmt = prepare("select distinct p.ethnicity from allele_frequency f join allele a on a.id = f.alleleid join population p on f.population = p.id where a.genesymbol=? order by 1");
      //language=PostgreSQL
      allelePopulationStmt = prepare("with x as (select frequency -> ? as frequency from allele where genesymbol=? and name=?) select * from x where x.frequency::text != 'null'");
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

    BigDecimal getFrequency(String gene, String alleleName, String ethnicity) throws SQLException {
      this.allelePopulationStmt.setString(2, gene);
      this.allelePopulationStmt.setString(3, alleleName);
      this.allelePopulationStmt.setString(1, ethnicity);
      try (ResultSet rs = this.allelePopulationStmt.executeQuery()) {
        if (rs.next()) {
          return rs.getBigDecimal(1);
        } else {
          return null;
        }
      }
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
