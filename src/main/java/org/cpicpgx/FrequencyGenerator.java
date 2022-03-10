package org.cpicpgx;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.cpicpgx.db.ConnectionFactory;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This class fills in the gaps of frequency data that is not present when filling out an allele frequency file for a
 * particular gene. Specifically, this is two major tasks:
 *
 * <ol>
 *   <li>synthesizing the frequency for the reference allele based on non-reference allele frequencies</li>
 *   <li>synthesizing frequencies for diplotypes and phenotypes based on allele frequencies</li>
 * </ol>
 *
 * This class should be safe to run multiple times. It will overwrite data in tables but it will always be the most
 * recent and accurate data.
 */
public class FrequencyGenerator {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final String f_gene;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("g", true,"gene to work on");
      CommandLineParser parser = new DefaultParser();
      CommandLine cli = parser.parse(options, args);
      String gene = cli.getOptionValue("g");

      FrequencyGenerator frequencyGenerator = new FrequencyGenerator(gene);
      frequencyGenerator.calculate();
    } catch (Exception ex) {
      sf_logger.error("Error generating frequency data", ex);
    }
  }

  public FrequencyGenerator(String gene) {
    f_gene = gene;
  }

  public void calculate() throws Exception {
    try (DataHarness dataHarness = new DataHarness(f_gene)) {

      // START Assign reference frequency
      Integer referenceAlleleId = dataHarness.lookupRefAlleleId();
      if (referenceAlleleId == null) {
        // if this is a gene with no reference allele then data cannot be calculated
        return;
      }
      sf_logger.debug("The ref allele ID is {} ({})", referenceAlleleId, dataHarness.getReferenceAlleleName());

      Integer count = dataHarness.lookupRefFreqCount(referenceAlleleId);
      sf_logger.debug("ref allele frequency count {}", count);

      Set<Integer> popIds = dataHarness.lookupPopulations();

      SortedMap<String,Integer> alleleMap = dataHarness.lookupAlleles();

      sf_logger.debug("make data for {}", popIds);

      for (Integer popId : popIds) {
        Float nonreferenceFrequency = dataHarness.lookupNonreferenceFrequency(popId);
        sf_logger.debug("pop {}, non-reference frequency {}", popId, nonreferenceFrequency);

        Float refFreq = 1f - nonreferenceFrequency;
        int results = dataHarness.writeReferenceFrequency(referenceAlleleId, popId, refFreq);
        if (results == 0) {
          throw new RuntimeException("No frequency data written");
        }
      }
      // END Assign reference frequency


      // START Assign allele + diplotype frequency
      JsonObject refAlleleFrequencyJson = new JsonObject();
      for (String alleleName : alleleMap.keySet()) {
        boolean isReference = referenceAlleleId.equals(alleleMap.get(alleleName));
        if (!isReference) {
          JsonObject alleleFrequencyJson = new JsonObject();
          for (String ethnicity : dataHarness.getEthnicitySet()) {
            Float freq = dataHarness.lookupFrequency(ethnicity, alleleMap.get(alleleName));
            alleleFrequencyJson.addProperty(ethnicity, freq);

            if (!refAlleleFrequencyJson.has(ethnicity)) {
              refAlleleFrequencyJson.addProperty(ethnicity, freq);
            } else {
              if (freq == null) {
                refAlleleFrequencyJson.add(ethnicity, refAlleleFrequencyJson.get(ethnicity));
              } else {
                if (refAlleleFrequencyJson.get(ethnicity).isJsonNull()) {
                  refAlleleFrequencyJson.addProperty(ethnicity, freq);
                } else {
                  refAlleleFrequencyJson.addProperty(ethnicity, freq + refAlleleFrequencyJson.get(ethnicity).getAsFloat());
                }
              }
            }
          }
          int rez = dataHarness.writeAlleleFrequency(alleleMap.get(alleleName), alleleFrequencyJson);
          if (rez == 0) {
            throw new RuntimeException("Missed write of allele frequency for " + alleleName);
          }
        }
      }
      for (String ethnicity : refAlleleFrequencyJson.keySet()) {
        refAlleleFrequencyJson.addProperty(ethnicity, 1f - refAlleleFrequencyJson.get(ethnicity).getAsFloat());
      }
      int rez = dataHarness.writeAlleleFrequency(referenceAlleleId, refAlleleFrequencyJson);
      if (rez == 0) {
        throw new RuntimeException("Missed write of allele frequency for allele ID " + referenceAlleleId);
      }

      dataHarness.updateDiplotypeFrequencies();
      // END Assign allele + diplotype frequency

      // START Assign phenotype frequency
      dataHarness.updatePhenotypeFrequencies();
      // END Assign phenotype frequency
    }
  }

  private static class DataHarness implements AutoCloseable {
    private final Connection conn;
    private final String geneSymbol;
    private Integer m_referenceAlleleId = null;
    private String m_referenceAlleleName = null;
    private final SortedSet<String> ethnicitySet = new TreeSet<>();
    private final SortedMap<String,Integer> alleleMap = new TreeMap<>(HaplotypeNameComparator.getComparator());
    private static final Gson gson = new Gson();

    PreparedStatement updateDiplotypeFrequency;
    PreparedStatement updateAlleleFrequency;
    PreparedStatement insertAlleleFrequency;
    PreparedStatement findAlleleFrequency;

    DataHarness(String geneSymbol) throws SQLException {
      conn = ConnectionFactory.newConnection();
      this.geneSymbol = geneSymbol;
      updateDiplotypeFrequency = conn.prepareStatement("update gene_result_diplotype set frequency=?::jsonb where id=?");
      insertAlleleFrequency = conn.prepareStatement(
          "insert into allele_frequency(alleleid, population, frequency, label) values (?, ?, ?, ?) " +
              "on conflict (alleleid, population) do update set frequency=excluded.frequency, label=excluded.label");
      updateAlleleFrequency = conn.prepareStatement("update allele set frequency=?::jsonb where id=?");
      findAlleleFrequency = conn.prepareStatement("select frequency -> ? from allele where genesymbol=? and name=?");
    }

    Integer lookupRefAlleleId() throws SQLException {
      PreparedStatement refAlleleStmt = conn.prepareStatement(
          "select a.id, a.name from allele a join allele_definition ad on a.definitionId=ad.id " +
              "where a.genesymbol=? and ad.reference = true and ad.name=a.name");
      refAlleleStmt.setString(1, this.geneSymbol);
      try (ResultSet rs = refAlleleStmt.executeQuery()) {
        boolean foundOne = false;
        while (rs.next()) {
          if (foundOne) {
            throw new RuntimeException("More than one reference allele found");
          }
          m_referenceAlleleId = rs.getInt(1);
          m_referenceAlleleName = rs.getString(2);
          sf_logger.debug("Found reference allele {}", m_referenceAlleleName);
          foundOne = true;
        }
      }
      return m_referenceAlleleId;
    }

    String getReferenceAlleleName() {
      return m_referenceAlleleName;
    }

    SortedSet<String> getEthnicitySet() {
      return ethnicitySet;
    }

    Integer lookupRefFreqCount(int alleleId) throws SQLException {
      PreparedStatement stmt = conn.prepareStatement(
          "select count(*) from allele_frequency where alleleid=? and frequency is not null");
      stmt.setInt(1, alleleId);
      int freqCount = 0;
      try (ResultSet rs = stmt.executeQuery()) {
        boolean foundOne = false;
        while (rs.next()) {
          if (foundOne) {
            throw new RuntimeException("More than one reference allele found");
          }
          freqCount = rs.getInt(1);
          foundOne = true;
        }
      }
      return freqCount;
    }

    Set<Integer> lookupPopulations() throws SQLException {
      PreparedStatement stmt = conn.prepareStatement("select distinct f.population, p.ethnicity\n" +
          "from allele_frequency f\n" +
          "    join allele a on f.alleleid = a.id\n" +
          "    join population p on f.population = p.id\n" +
          "where a.genesymbol=?");
      stmt.setString(1, geneSymbol);
      Set<Integer> popIds = new HashSet<>();
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          popIds.add(rs.getInt(1));
          ethnicitySet.add(rs.getString(2));
        }
      }
      return popIds;
    }

    SortedMap<String,Integer> lookupAlleles() throws SQLException {
      PreparedStatement stmt = conn.prepareStatement("select id,name from allele a where a.genesymbol=?");
      stmt.setString(1, geneSymbol);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          alleleMap.put(rs.getString(2), rs.getInt(1));
        }
      }
      return alleleMap;
    }

    Float lookupNonreferenceFrequency(Integer popId) throws SQLException {
      PreparedStatement stmt = conn.prepareStatement("select sum(f.frequency)\n" +
          "from allele_frequency f\n" +
          "         join allele a on f.alleleid = a.id\n" +
          "join allele_definition ad on a.definitionid = ad.id\n" +
          "         join population p on f.population = p.id\n" +
          "where a.genesymbol=? and f.population=? and ad.reference=false");
      stmt.setString(1, geneSymbol);
      stmt.setInt(2, popId);
      Float freq = null;
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          freq = rs.getFloat(1);
        }
      }
      return freq;
    }

    @Nullable
    Float lookupFrequency(String ethnicity, Integer alleleId) throws SQLException {
      PreparedStatement stmt = conn.prepareStatement("SELECT" +
          "       sum(p.subjectcount)," +
          "       sum(p.subjectcount::numeric * af.frequency / 100::numeric) / sum(p.subjectcount)::numeric * 100::numeric," +
          "       bool_or(af.frequency is not null) " +
          "FROM population p" +
          "         JOIN allele_frequency af ON p.id = af.population" +
          "         JOIN allele a ON af.alleleid = a.id " +
          "WHERE af.frequency IS NOT NULL and af.alleleid=? and p.ethnicity=?");
      stmt.setInt(1, alleleId);
      stmt.setString(2, ethnicity);
      Float freq = null;
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          boolean hasData = rs.getBoolean(3);
          if (hasData) {
            freq = rs.getFloat(2);
          }
        }
        if (rs.next()) {
          throw new RuntimeException("Single result expected");
        }
      }
      return freq;
    }

    @Nullable
    Float findFrequency(String alleleName, String ethnicity) throws SQLException {
      findAlleleFrequency.setString(1, ethnicity);
      findAlleleFrequency.setString(2, geneSymbol);
      findAlleleFrequency.setString(3, alleleName);
      Float freqeuncy = null;
      try (ResultSet r = findAlleleFrequency.executeQuery()) {
        if (r.next()) {
          Object freqObj = r.getObject(1);
          if (freqObj != null) {
            try {
              freqeuncy = r.getFloat(1);
            } catch (PSQLException ex) {
              // drop and skip
            }
          }
        }
        if (r.next()) {
          throw new RuntimeException("More than one result returned");
        }
      }
      return freqeuncy;
    }

    void updateDiplotypeFrequencies() throws SQLException {
      PreparedStatement stmt = conn.prepareStatement(
          "select grd.id, grd.diplotypekey from gene_result r " +
              "join gene_result_lookup grl on r.id = grl.phenotypeid " +
              "join gene_result_diplotype grd on grl.id = grd.functionphenotypeid " +
              "where r.genesymbol=?");
      stmt.setString(1, geneSymbol);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int diplotypeId = rs.getInt(1);
          String diplotyepKeyString = rs.getString(2);

          JsonObject diplotypeKey = gson.fromJson(diplotyepKeyString, JsonObject.class);
          JsonObject frequencyByEthnicityObject = new JsonObject();
          Set<String> alleles = diplotypeKey.getAsJsonObject(geneSymbol).keySet();
          if (alleles.size() > 2) {
            throw new RuntimeException("Unexpected diplotype state: " + diplotyepKeyString);
          } else if (alleles.size() == 1) {
            String alleleName = alleles.stream().findFirst().orElseThrow(() -> new RuntimeException("Allele not found"));
            for (String ethnicity : ethnicitySet) {
              Float individualFrequency = findFrequency(alleleName, ethnicity);
              Float diplotypeFrequency = null;
              if (individualFrequency != null) {
                diplotypeFrequency = individualFrequency * individualFrequency;
              }
              frequencyByEthnicityObject.addProperty(ethnicity, diplotypeFrequency);
            }
            writeDiplotypeFrequency(diplotypeId, frequencyByEthnicityObject);
          } else {
            String[] alleleNameArray = new String[alleles.size()];
            int i=0;
            for (String allele : alleles) {
              alleleNameArray[i++] = allele;
            }
            for (String ethnicity : ethnicitySet) {
              Float[] alleleFreqArray = new Float[]{
                  findFrequency(alleleNameArray[0], ethnicity),
                  findFrequency(alleleNameArray[1], ethnicity)
              };
              Float freq = null;
              if (alleleFreqArray[0] != null && alleleFreqArray[1] != null) {
                freq = alleleFreqArray[0] * alleleFreqArray[1] * 2;
              }
              frequencyByEthnicityObject.addProperty(ethnicity, freq);
            }
            int result = writeDiplotypeFrequency(diplotypeId, frequencyByEthnicityObject);
            if (result != 1) {
              throw new RuntimeException("Unexpected failure to update diplotype");
            }
          }
        }
      }
    }

    void updatePhenotypeFrequencies() throws SQLException {
      PreparedStatement dipStmt = conn.prepareStatement("select sum(f.value::numeric) " +
          "from gene_result r " +
          "    join gene_result_lookup grl on r.id = grl.phenotypeid " +
          "    join gene_result_diplotype grd on grl.id = grd.functionPhenotypeId, " +
          "     jsonb_each(grd.frequency) f " +
          "where f.key=? and r.id=? and f.value::text!='null'");
      PreparedStatement updateStmt = conn.prepareStatement("update gene_result set frequency=?::jsonb where id=?");
      PreparedStatement stmt = conn.prepareStatement("select id from gene_result where genesymbol=?");
      stmt.setString(1, geneSymbol);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          int phenotypeId = rs.getInt(1);
          JsonObject frequencyObject = new JsonObject();
          for (String ethnicity : ethnicitySet) {
            dipStmt.setString(1, ethnicity);
            dipStmt.setInt(2, phenotypeId);
            try (ResultSet dipRs = dipStmt.executeQuery()) {
              while (dipRs.next()) {
                Float sumFreq = dipRs.getFloat(1);
                frequencyObject.addProperty(ethnicity, sumFreq);
              }
            }
          }
          updateStmt.setString(1, frequencyObject.toString());
          updateStmt.setInt(2, phenotypeId);
          int result = updateStmt.executeUpdate();
          if (result != 1) {
            throw new RuntimeException("Unexpected failure to update phenotype");
          }
        }
      }
    }

    int writeReferenceFrequency(Integer alleleId, Integer popId, Float freq) throws SQLException {
      sf_logger.debug("pop {}, reference frequency: {}", popId, freq);

      insertAlleleFrequency.setInt(1, alleleId);
      insertAlleleFrequency.setInt(2, popId);
      insertAlleleFrequency.setFloat(3, freq);
      insertAlleleFrequency.setString(4, freq.toString());
      return insertAlleleFrequency.executeUpdate();
    }

    int writeDiplotypeFrequency(Integer diplotypeId, JsonObject frequencyObject) throws SQLException {
      updateDiplotypeFrequency.setString(1, frequencyObject.toString());
      updateDiplotypeFrequency.setInt(2, diplotypeId);
      return updateDiplotypeFrequency.executeUpdate();
    }

    int writeAlleleFrequency(Integer alleleId, JsonObject frequency) throws SQLException {
      updateAlleleFrequency.setString(1, frequency.toString());
      updateAlleleFrequency.setInt(2, alleleId);
      return updateAlleleFrequency.executeUpdate();
    }

    @Override
    public void close() throws Exception {
      conn.close();
    }
  }
}
