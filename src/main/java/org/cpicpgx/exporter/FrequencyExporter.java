package org.cpicpgx.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.ActivityScoreComparator;
import org.cpicpgx.util.DbHarness;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sawano.java.text.AlphanumericComparator;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

/**
 * Exports a frequency excel sheet for every gene in the database that has frequency data
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
              "select distinct a.name, a.id, ad.reference from allele_frequency f join allele a on f.alleleid = a.id join allele_definition ad on a.definitionid = ad.id where a.genesymbol=? order by 1");
          PreparedStatement popsStmt = conn.prepareStatement(
              "select distinct coalesce(p2.pmid, p2.url, p2.pmcid, p2.doi), p.ethnicity, p.population, p.populationinfo, p.subjecttype, p2.authors, p2.year, p.id, p.subjectcount\n" +
              "from allele_frequency f join population p on f.population = p.id join allele a on f.alleleid = a.id\n" +
              "left join publication p2 on p.publicationId=p2.id\n" +
              "where a.genesymbol=? and p.ethnicity=? order by p.ethnicity, p2.year, p2.authors, p.population");
          PreparedStatement afStmt = conn.prepareStatement(
              "select f.label, f.frequency from allele_frequency f where f.population=? and f.alleleid=?");
          PreparedStatement ethStmt = conn.prepareStatement(
              "select distinct population_group " +
                  "from population_frequency_view v where v.population_group != 'n/a' and v.genesymbol=?");
          PreparedStatement ethAlleleStmt = conn.prepareStatement(
              "select freq_weighted_avg, freq_max, freq_min from population_frequency_view v where v.name=? and v.population_group=? and v.genesymbol=?"
          );
          PreparedStatement ethRefStmt = conn.prepareStatement(
              "select 1-sum(freq_weighted_avg) as ref_freq from population_frequency_view v where v.population_group=? and v.genesymbol=?"
          );
          PreparedStatement methodsStmt = conn.prepareStatement(
              "select frequencyMethods from gene where symbol=?"
          );
          PreparedStatement geneStmt = conn.prepareStatement(
              "select distinct a.geneSymbol, g.lookupmethod, ad.name from allele_frequency f join allele a on f.alleleid = a.id join gene g on a.genesymbol = g.symbol join allele_definition ad on g.symbol = ad.genesymbol where ad.reference is true order by 1");
          ResultSet geneResults = geneStmt.executeQuery()
      ) {
        // gene loop
        while (geneResults.next()) {
          String geneSymbol = geneResults.getString(1);
          LookupMethod lookupMethod = LookupMethod.valueOf(geneResults.getString(2));
          String refAlleleName = geneResults.getString(3);
          FrequencyWorkbook workbook = new FrequencyWorkbook(geneSymbol, lookupMethod);


          // start the Allele Frequency sheet
          List<String> allelePops = dbHarness.getAllelePopulations(geneSymbol);
          Set<String> alleleNames = dbHarness.getAllelesWithFrequencies(geneSymbol);

          if (allelePops.size() > 0) {
            workbook.writeAlleleFrequencyHeader(allelePops);

            // infer reference allele values based on other alleles
            if (!alleleNames.contains(refAlleleName)) {
              Double[] frequencies = new Double[allelePops.size()];
              for (String pop : allelePops) {
                frequencies[allelePops.indexOf(pop)] = dbHarness.getInferredReferenceFrequencyForPopulation(geneSymbol, pop);
              }
              workbook.writeAlleleFrequency(refAlleleName, frequencies);
            }

            for (String allele : alleleNames) {
              Double[] frequencies = new Double[allelePops.size()];
              for (String pop : allelePops) {
                frequencies[allelePops.indexOf(pop)] = dbHarness.getFrequency(geneSymbol, allele, pop);
              }
              workbook.writeAlleleFrequency(allele, frequencies);
            }
          }
          // end the Allele Frequency sheet


          // start the Diplotype Frequency sheet
          List<String> dipPops = dbHarness.getDiplotypePopulations(geneSymbol);
          if (dipPops.size() > 0) {
            workbook.writeDiplotypeFrequencyHeader(dipPops);
            Map<String, HashMap<String,Double>> diplotypeMap = dbHarness.getDiplotypeData(geneSymbol);

            for (String diplotype : diplotypeMap.keySet()) {
              Double[] frequencies = new Double[dipPops.size()];
              Map<String,Double> popMap = diplotypeMap.get(diplotype);
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
          // end the Diplotype Frequency sheet


          // start the Phenotype Frequency sheet
          List<String> phenoPops = dbHarness.getDiplotypePopulations(geneSymbol);
          if (phenoPops.size() > 0) {
            workbook.writePhenotypeFrequencyHeader(phenoPops);
            Map<String, HashMap<String,Double>> phenotypeMap = dbHarness.getPhenotypeData(geneSymbol, lookupMethod);

            for (String phenotype : phenotypeMap.keySet()) {
              Double[] frequencies = new Double[phenoPops.size()];
              Map<String,Double> popMap = phenotypeMap.get(phenotype);
              if (popMap != null) {
                for (String pop : phenoPops) {
                  int idx = phenoPops.indexOf(pop);
                  if (idx > -1) {
                    frequencies[idx] = popMap.get(pop);
                  }
                }
              }
              workbook.writePhenotypeFrequency(phenotype, frequencies);
            }
          }
          // end the Phenotype Frequency sheet


          // get the ethnicities applicable to this gene
          ethStmt.setString(1, geneSymbol);
          SortedSet<String> ethnicities = new TreeSet<>();
          try (ResultSet eth = ethStmt.executeQuery()) {
            while (eth.next()) {
              ethnicities.add(eth.getString(1));
            }
          }

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
                      double freq = afrs.getDouble(2);
                      if (freq != 0) {
                        frequencies[i] = String.format("%.4f", freq);
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

            ethRefStmt.setString(1, ethnicity);
            ethRefStmt.setString(2, geneSymbol);
            Double refAvg = null;
            try (ResultSet rsRef = ethRefStmt.executeQuery()) {
              while (rsRef.next()) {
                refAvg = rsRef.getDouble(1);
              }
            }

            workbook.startPopulationSummary();
            for (String allele : alleles.keySet()) {
              ethAlleleStmt.setString(1, allele);
              ethAlleleStmt.setString(2, ethnicity);
              ethAlleleStmt.setString(3, geneSymbol);
              try (ResultSet rsEth = ethAlleleStmt.executeQuery()) {
                boolean wroteSummary = false;
                while (rsEth.next()) {
                  workbook.writePopulationSummary(rsEth.getDouble(3), rsEth.getDouble(1), rsEth.getDouble(2));
                  wroteSummary = true;
                }
                if (!wroteSummary) {
                  if (allele.equals(refAllele)) {
                    workbook.writeReferencePopulationSummary(refAvg);
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
    @SuppressWarnings("UnstableApiUsage")
    final Type doubleMapType = new TypeToken<HashMap<String, Double>>(){}.getType();
    PreparedStatement populationsStmt;
    PreparedStatement allelePopulationStmt;
    PreparedStatement inferredRefFreqStmt;
    PreparedStatement diplotypePopStmt;
    PreparedStatement diplotypeDataStmt;
    PreparedStatement phenotypePopStmt;
    PreparedStatement phenotypeDataStmt;
    PreparedStatement activityDataStmt;
    PreparedStatement alleleNameStmt;

    FrequencyDbHarness() throws SQLException {
      super(FileType.FREQUENCY);

      //language=PostgreSQL
      populationsStmt = prepare("select distinct population_group from population_frequency_view where genesymbol=? and freq_weighted_avg is not null order by 1");
      //language=PostgreSQL
      allelePopulationStmt = prepare("select freq_weighted_avg from population_frequency_view where genesymbol=? and name=? and population_group=?");
      //language=PostgreSQL
      inferredRefFreqStmt = prepare("select 1-sum(freq_weighted_avg) freq_weighted, 1-sum(freq_avg) freq from population_frequency_view f where f.genesymbol=? and population_group=?");
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
      alleleNameStmt = prepare("select distinct name from population_frequency_view where genesymbol=?");
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

    Double getFrequency(String gene, String alleleName, String bioGroup) throws SQLException {
      this.allelePopulationStmt.setString(1, gene);
      this.allelePopulationStmt.setString(2, alleleName);
      this.allelePopulationStmt.setString(3, bioGroup);
      try (ResultSet rs = this.allelePopulationStmt.executeQuery()) {
        if (rs.next()) {
          return rs.getDouble(1);
        } else {
          return null;
        }
      }
    }

    Double getInferredReferenceFrequencyForPopulation(String gene, String bioGroup) throws SQLException {
      this.inferredRefFreqStmt.setString(1, gene);
      this.inferredRefFreqStmt.setString(2, bioGroup);
      try (ResultSet rs = this.inferredRefFreqStmt.executeQuery()) {
        if (rs.next()) {
          return rs.getDouble(1);
        } else {
          throw new RuntimeException("Not able to infer reference allele frequency");
        }
      }
    }

    List<String> getAllelePopulations(String gene) throws SQLException {
      List<String> result = new ArrayList<>();
      if (StringUtils.isNotBlank(gene)) {
        this.populationsStmt.clearParameters();
        this.populationsStmt.setString(1, gene);
        try (ResultSet rs = this.populationsStmt.executeQuery()) {
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

    Map<String, HashMap<String,Double>> getDiplotypeData(String gene) throws SQLException {
      Map<String, HashMap<String,Double>> result = new TreeMap<>(HaplotypeNameComparator.getComparator());
      if (StringUtils.isNotBlank(gene)) {
        this.diplotypeDataStmt.clearParameters();
        this.diplotypeDataStmt.setString(1, gene);
        try (ResultSet rs = this.diplotypeDataStmt.executeQuery()) {
          while (rs.next()) {
            result.put(rs.getString(1), gson.fromJson(rs.getString(2), doubleMapType));
          }
        }
      }
      return result;
    }

    Map<String, HashMap<String,Double>> getPhenotypeData(String gene, LookupMethod lookupMethod) throws SQLException {
      Comparator<String> comparator = lookupMethod == LookupMethod.ACTIVITY_SCORE ? ActivityScoreComparator.getComparator() : Comparator.reverseOrder();
      PreparedStatement query = lookupMethod == LookupMethod.ACTIVITY_SCORE ? this.activityDataStmt : phenotypeDataStmt;
      Map<String, HashMap<String,Double>> result = new TreeMap<>(comparator);
      if (StringUtils.isNotBlank(gene)) {
        query.clearParameters();
        query.setString(1, gene);
        try (ResultSet rs = query.executeQuery()) {
          while (rs.next()) {
            result.put(rs.getString(1), gson.fromJson(rs.getString(2), doubleMapType));
          }
        }
      }
      return result;
    }
  }
}
