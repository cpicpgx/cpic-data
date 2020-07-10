package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Exports a frequency excel sheet for every gene in the database that has frequency data
 *
 * @author Ryan Whaley
 */
public class FrequencyExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern REF_ALLELE_PATTERN = Pattern.compile("^(\\*1|.*[Rr]eference.*)$");

  public static void main(String[] args) {
    FrequencyExporter exporter = new FrequencyExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting frequencies", ex);
    }
  }

  FileType getFileType() {
    return FileType.FREQUENCIES;
  }

  EntityType getEntityCategory() {
    return EntityType.GENE;
  }
  
  @Override
  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection()) {
      try (
          PreparedStatement pstmt = conn.prepareStatement(
              "select distinct geneSymbol from population_frequency_view order by 1");
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
          PreparedStatement refFreqStmt = conn.prepareStatement(
              "select 1 - sum(freq_weighted_avg) reference_freq from population_frequency_view where name!=? and population_group=? and genesymbol=?");
          PreparedStatement changeStmt = conn.prepareStatement(
              "select n.date, note from gene_note n where type='"+ NoteType.FUNCTION_REFERENCE +"' and genesymbol=? and n.date is not null order by ordinal"
          );
          PreparedStatement geneStmt = conn.prepareStatement(
              "select frequencyMethods from gene where symbol=?"
          );
          ResultSet rs = pstmt.executeQuery()
      ) {
        // gene loop
        while (rs.next()) {
          String geneSymbol = rs.getString(1);
          FrequencyWorkbook workbook = new FrequencyWorkbook(geneSymbol);

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

          workbook.writeEthnicity();
          for (String allele : alleles.keySet()) {
            PreparedStatement specificStmt;
            if (REF_ALLELE_PATTERN.matcher(allele).matches()) {
              specificStmt = refFreqStmt;
            } else {
              specificStmt = ethAlleleStmt;
            }

            Double[] frequencies = new Double[ethnicities.size()];
            int i = 0;
            for (String ethnicity : ethnicities) {
              specificStmt.clearParameters();
              specificStmt.setString(1, allele);
              specificStmt.setString(2, ethnicity);
              specificStmt.setString(3, geneSymbol);
              try (ResultSet ethAllele = specificStmt.executeQuery()) {
                while (ethAllele.next()) {
                  double freq = ethAllele.getDouble(1);
                  frequencies[i] = freq;
                }
              }
              i += 1;
            }
            workbook.writeEthnicitySummary(allele, frequencies);
          }

          // writing the notes
          changeStmt.setString(1, geneSymbol);
          try (ResultSet notes = changeStmt.executeQuery()) {
            while (notes.next()) {
              workbook.writeHistory(notes.getDate(1), notes.getString(2));
            }
          }

          // writing the methods for this gene
          geneStmt.setString(1, geneSymbol);
          String methods;
          try (ResultSet grs = geneStmt.executeQuery()) {
            if (grs.next()) {
              methods = grs.getString(1);
            } else {
              throw new RuntimeException("No methods text found in DB");
            }
          }
          workbook.writeMethods(methods);

          writeWorkbook(workbook);
          addFileExportHistory(workbook.getFilename(), new String[]{geneSymbol});
        }
      }
      handleFileUpload();
    }
  }
}
