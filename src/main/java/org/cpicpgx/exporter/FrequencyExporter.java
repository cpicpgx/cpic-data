package org.cpicpgx.exporter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.DbHarness;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
              "select distinct a.name, a.id from allele_frequency f join allele a on f.alleleid = a.id where a.genesymbol=? order by 1");
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
          PreparedStatement methodsStmt = conn.prepareStatement(
              "select frequencyMethods from gene where symbol=?"
          );
          PreparedStatement geneStmt = conn.prepareStatement(
              "select distinct geneSymbol from population_frequency_view order by 1");
          ResultSet geneResults = geneStmt.executeQuery()
      ) {
        // gene loop
        while (geneResults.next()) {
          String geneSymbol = geneResults.getString(1);
          FrequencyWorkbook workbook = new FrequencyWorkbook(geneSymbol);


          // start the Allele Frequency sheet
          List<String> allelePops = dbHarness.getAllelePopulations(geneSymbol);
          if (allelePops.size() > 0) {
            workbook.writeAlleleFrequencyHeader(allelePops);
            Map<String, HashMap<String,Double>> alleleMap = dbHarness.getAlleleData(geneSymbol);

            for (String allele : alleleMap.keySet()) {
              Double[] frequencies = new Double[allelePops.size()];
              Map<String,Double> popMap = alleleMap.get(allele);
              for (String pop : allelePops) {
                frequencies[allelePops.indexOf(pop)] = popMap.get(pop);
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
            Map<String, HashMap<String,Double>> phenotypeMap = dbHarness.getPhenotypeData(geneSymbol);

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
          stmt.setString(1, geneSymbol);
          try (ResultSet r = stmt.executeQuery()) {
            while (r.next()) {
              alleles.put(r.getString(1), r.getInt(2));
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
    PreparedStatement allelePopStmt;
    PreparedStatement alleleDataStmt;
    PreparedStatement diplotypePopStmt;
    PreparedStatement diplotypeDataStmt;
    PreparedStatement phenotypePopStmt;
    PreparedStatement phenotypeDataStmt;

    FrequencyDbHarness() throws SQLException {
      super(FileType.FREQUENCY);

      //language=PostgreSQL
      allelePopStmt = prepare("select distinct jsonb_object_keys(frequency) from allele where genesymbol=? and frequency is not null order by 1");
      //language=PostgreSQL
      alleleDataStmt = prepare("select name,frequency from allele where genesymbol=? and frequency is not null");
      //language=PostgreSQL
      diplotypePopStmt = prepare("select distinct jsonb_object_keys(grd.frequency) from gene_result r join gene_result_lookup grl on r.id = grl.phenotypeid join gene_result_diplotype grd on grl.id = grd.functionphenotypeid where r.genesymbol=? and grd.frequency is not null order by 1");
      //language=PostgreSQL
      diplotypeDataStmt = prepare("select grd.diplotype, grd.frequency from gene_result r join gene_result_lookup grl on r.id = grl.phenotypeid join gene_result_diplotype grd on grl.id = grd.functionphenotypeid where r.genesymbol=?");
      //language=PostgreSQL
      phenotypePopStmt = prepare("select distinct jsonb_object_keys(frequency) from gene_result where genesymbol=? and frequency is not null order by 1");
      //language=PostgreSQL
      phenotypeDataStmt = prepare("select result,frequency from gene_result where genesymbol=? and frequency is not null order by result desc");
    }

    List<String> getAllelePopulations(String gene) throws SQLException {
      List<String> result = new ArrayList<>();
      if (StringUtils.isNotBlank(gene)) {
        this.allelePopStmt.clearParameters();
        this.allelePopStmt.setString(1, gene);
        try (ResultSet rs = this.allelePopStmt.executeQuery()) {
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

    Map<String, HashMap<String,Double>> getAlleleData(String gene) throws SQLException {
      Map<String, HashMap<String,Double>> result = new TreeMap<>(HaplotypeNameComparator.getComparator());
      if (StringUtils.isNotBlank(gene)) {
        this.alleleDataStmt.clearParameters();
        this.alleleDataStmt.setString(1, gene);
        try (ResultSet rs = this.alleleDataStmt.executeQuery()) {
          while (rs.next()) {
            result.put(rs.getString(1), gson.fromJson(rs.getString(2), doubleMapType));
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

    Map<String, HashMap<String,Double>> getPhenotypeData(String gene) throws SQLException {
      Map<String, HashMap<String,Double>> result = new TreeMap<>(Comparator.naturalOrder());
      if (StringUtils.isNotBlank(gene)) {
        this.phenotypeDataStmt.clearParameters();
        this.phenotypeDataStmt.setString(1, gene);
        try (ResultSet rs = this.phenotypeDataStmt.executeQuery()) {
          while (rs.next()) {
            result.put(rs.getString(1), gson.fromJson(rs.getString(2), doubleMapType));
          }
        }
      }
      return result;
    }
  }
}
