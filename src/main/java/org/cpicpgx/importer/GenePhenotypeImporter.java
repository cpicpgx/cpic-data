package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import se.sawano.java.text.AlphanumericComparator;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses gene phenotype data from workbooks meant to mirror Table 1 information from guidelines
 *
 * @author Ryan Whaley
 */
public class GenePhenotypeImporter extends BaseDirectoryImporter {
  private static final String FILE_SUFFIX = "_phenotypes.xlsx";
  private static final String[] sf_deleteStatements = new String[]{
      "delete from phenotype_diplotype",
      "delete from phenotype_function",
      "delete from gene_phenotype"
  };
  private static final String DEFAULT_DIRECTORY = "gene_phenotypes";
  private static final Pattern GENE_PATTERN = Pattern.compile("Gene:\\s+(\\w+)");

  private static final int COL_A1_FN = 0;
  private static final int COL_A2_FN = 1;
  private static final int COL_A1_VALUE = 2;
  private static final int COL_A2_VALUE = 3;
  private static final int COL_TOTAL_SCORE = 4;
  private static final int COL_PHENO = 5;
  private static final int COL_DESC = 6;

  public static void main(String[] args) {
    rebuild(new GenePhenotypeImporter(), args);
  }

  @Override
  String getFileExtensionToProcess() {
    return FILE_SUFFIX;
  }

  @Override
  FileType getFileType() {
    return FileType.GENE_PHENOTYPE;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    workbook.switchToSheet(0);

    RowWrapper geneRow = workbook.getRow(0);
    if (geneRow.hasNoText(0)) {
      throw new RuntimeException("Gene not specified");
    }
    Matcher m = GENE_PATTERN.matcher(geneRow.getNullableText(0));
    if (!m.find()) {
      throw new RuntimeException("Gene value in unrecognized format");
    }
    String geneSymbol = m.group(1);

    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      for (int i = 2; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper dataRow = workbook.getRow(i);
        if (dataRow.hasNoText(0)) {
          continue;
        }
        try {
          dbHarness.insertValues(dataRow);
        } catch (Exception e) {
          throw new RuntimeException("Error processing row " + (i+1), e);
        }
      }
    }
  }

  static class DbHarness implements AutoCloseable {
    private final Connection conn;
    private final PreparedStatement insertPhenotype;
    private final PreparedStatement insertFunction;
    private final PreparedStatement lookupDiplotypes;
    private final PreparedStatement lookupDiplotypesByScore;
    private final PreparedStatement insertDiplotype;
    private final String geneSymbol;
    private final Map<String, Integer> phenotypeCache = new HashMap<>();
    private boolean useScoreLookup = false;
    private final Set<String> loadedDiplotypes = new HashSet<>();

    DbHarness(String geneSymbol) throws SQLException {
      this.geneSymbol = geneSymbol;
      this.conn = ConnectionFactory.newConnection();
      this.insertPhenotype = conn.prepareStatement("insert into gene_phenotype(genesymbol, phenotype, activityScore) values (?, ?, ?) returning id");
      this.insertFunction = conn.prepareStatement("insert into phenotype_function(phenotypeid, functionkey, function1, function2, activityvalue1, activityvalue2, totalactivityscore, description) values (?, ?::jsonb, ?, ?, ?, ?, ?, ?) returning id");
      this.lookupDiplotypes = conn.prepareStatement("select a1.name, a2.name " +
          "from gene_phenotype g join phenotype_function pf on g.id = pf.phenotypeid " +
          "                      join allele a1 on g.genesymbol = a1.genesymbol and a1.clinicalfunctionalstatus=pf.function1 " +
          "                      join allele a2 on g.genesymbol = a2.genesymbol and a2.clinicalfunctionalstatus=pf.function2 " +
          "where pf.id=? order by a1.name, a2.name");
      this.lookupDiplotypesByScore = conn.prepareStatement("select a1.name, a2.name " +
          "from gene_phenotype g join phenotype_function pf on g.id = pf.phenotypeid " +
          "                      join allele a1 on g.genesymbol = a1.genesymbol and a1.activityvalue=pf.activityvalue1 " +
          "                      join allele a2 on g.genesymbol = a2.genesymbol and a2.activityvalue=pf.activityvalue2 " +
          "where pf.id=? order by a1.name, a2.name");
      this.insertDiplotype = conn.prepareStatement("insert into phenotype_diplotype(functionphenotypeid, diplotype, diplotypekey) values (?, ?, ?::jsonb)");

      try (PreparedStatement lookupGene = conn.prepareStatement("select lookupMethod from gene where symbol=?")) {
        lookupGene.setString(1, geneSymbol);
        try (ResultSet rs = lookupGene.executeQuery()) {
          while (rs.next()) {
            LookupMethod lookupMethod = LookupMethod.valueOf(rs.getString(1));
            if (lookupMethod == LookupMethod.ACTIVITY_SCORE) {
              this.useScoreLookup = true;
            }
          }
        }
      }
    }

    void insertValues(RowWrapper row) throws SQLException {
      String a1Fn = row.getText(COL_A1_FN);
      String a2Fn = row.getText(COL_A2_FN);
      String a1Value = Optional.ofNullable(normalizeScore(row.getNullableText(COL_A1_VALUE))).orElse(NA);
      String a2Value = Optional.ofNullable(normalizeScore(row.getNullableText(COL_A2_VALUE))).orElse(NA);
      String totalScore = Optional.ofNullable(normalizeScore(row.getNullableText(COL_TOTAL_SCORE))).orElse(NA);
      String description = row.getNullableText(COL_DESC);
      int phenoId = lookupPhenotype(row.getText(COL_PHENO), totalScore);

      validateScoreData(a1Value, a2Value, totalScore);

      this.insertFunction.setInt(1, phenoId);
      if (this.useScoreLookup) {
        this.insertFunction.setString(2, makeFunctionKey(null, null, a1Value, a2Value));
      } else {
        this.insertFunction.setString(2, makeFunctionKey(a1Fn, a2Fn, null, null));
      }
      this.insertFunction.setString(3, a1Fn);
      this.insertFunction.setString(4, a2Fn);
      this.insertFunction.setString(5, a1Value);
      this.insertFunction.setString(6, a2Value);
      this.insertFunction.setString(7, totalScore);
      if (description != null) {
        this.insertFunction.setString(8, description);
      } else {
        this.insertFunction.setNull(8, Types.VARCHAR);
      }

      try (ResultSet rs = this.insertFunction.executeQuery()) {
        if (rs.next()) {
          int fnId = rs.getInt(1);
          insertDiplotypes(fnId);
        } else {
          throw new RuntimeException("Couldn't insert function");
        }
      }
    }

    /**
     * Generates phenotype_diplotype records based off of data about phenotypes in the
     * @param functionId the primary key ID for a phenotype
     * @throws SQLException can occur when inserting into the DB
     */
    void insertDiplotypes(int functionId) throws SQLException {
      PreparedStatement lookup = this.useScoreLookup ? this.lookupDiplotypesByScore : this.lookupDiplotypes;

      lookup.setInt(1, functionId);
      Set<List<String>> rawDiplotypes = new HashSet<>();

      // caching the raw diplotypes ahead of time so they can be normalized. For example, *1/*2 is the same as *2/*1 so
      // this code should reduce those two records to just one of *1/*2 thanks to the AlphanumericComparator
      try (ResultSet rs = lookup.executeQuery()) {
        while (rs.next()) {
          List<String> rawDiplotype = new ArrayList<>();
          rawDiplotype.add(rs.getString(1));
          rawDiplotype.add(rs.getString(2));
          rawDiplotype.sort(new AlphanumericComparator(Locale.ENGLISH));
          rawDiplotypes.add(rawDiplotype);
        }
      }

      for (List<String> rawDiplotype : rawDiplotypes) {
        String a1 = rawDiplotype.get(0);
        String a2 = rawDiplotype.get(1);

        String diplotypeText = String.format("%s/%s", a1, a2);
        if (!this.loadedDiplotypes.contains(diplotypeText)) {
          JsonObject diplotypeKey = new JsonObject();
          if (a1.equals(a2)) {
            diplotypeKey.addProperty(a1, 2);
          } else {
            diplotypeKey.addProperty(a1, 1);
            diplotypeKey.addProperty(a2, 1);
          }

          this.insertDiplotype.setInt(1, functionId);
          this.insertDiplotype.setString(2, diplotypeText);
          this.insertDiplotype.setString(3, diplotypeKey.toString());
          this.insertDiplotype.executeUpdate();
          this.loadedDiplotypes.add(diplotypeText);
        }
      }
    }

    int lookupPhenotype(String phenotype, String score) throws SQLException {
      String normalizedPhenotype = phenotype.replaceAll(this.geneSymbol + "\\s", "");
      String normalizedScore = normalizeScore(score);
      String lookupKey;
      if (useScoreLookup) {
        lookupKey = normalizedScore;
      } else {
        lookupKey = normalizedPhenotype;
      }

      if (phenotypeCache.containsKey(lookupKey)) {
        return phenotypeCache.get(lookupKey);
      } else {
        this.insertPhenotype.setString(1, this.geneSymbol);
        this.insertPhenotype.setString(2, normalizedPhenotype);
        this.insertPhenotype.setString(3, normalizedScore);

        try (ResultSet rs = this.insertPhenotype.executeQuery()) {
          if (rs.next()) {
            int phenoId = rs.getInt(1);
            this.phenotypeCache.put(lookupKey, phenoId);
            return phenoId;
          } else {
            throw new RuntimeException("Couldn't insert phenotype");
          }
        }
      }
    }

    /**
     * Do some quick validation of score values if they are present
     * @param score1 first score
     * @param score2 second score
     * @param total the total of the first and second score
     */
    private void validateScoreData(String score1, String score2, String total) {
      // if score is not used, nothing to do
      if (!this.useScoreLookup) return;

      // if score data is missing throw an exception
      if (score1 == null || score2 == null || total == null) {
        throw new RuntimeException("Score data is missing");
      }

      // if "n/a" values are present
      if (score1.equalsIgnoreCase(NA) || score2.equalsIgnoreCase(NA)) {
        // the total must be "n/a"
        if (!total.equalsIgnoreCase(NA)){
          throw new RuntimeException("n/a score is not consistent");
        } else {
          // nothing left to check for n/a values
          return;
        }
      }

      // do some simple math to make sure the values are consistent
      if (convertScore(score1) + convertScore(score2) != convertScore(total)) {
        throw new RuntimeException("scores don't add up to total");
      }
    }

    /**
     * Try to convert a string score into a computable double
     * @param score a score that may possibly contain a ≥ character
     * @return a double representation of a score
     */
    private static double convertScore(String score) {
      String strippedScore = score.replaceAll("≥", "");
      return Double.parseDouble(strippedScore);
    }

    @Override
    public void close() throws Exception {
      if (this.conn != null) {
        this.conn.close();
      }
    }
  }
}
