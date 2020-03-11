package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.cpicpgx.db.ConnectionFactory;
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
  private static final String DEFAULT_DIRECTORY = "recommendation_tables";
  private static final Pattern GENE_PATTERN = Pattern.compile("Gene:\\s+(\\w+)");

  private static final int COL_A1_FN = 0;
  private static final int COL_A2_FN = 1;
  private static final int COL_A1_SCORE = 2;
  private static final int COL_A2_SCORE = 3;
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

    try (DbHarness dbHarness = new DbHarness()) {
      for (int i = 2; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper dataRow = workbook.getRow(i);
        if (dataRow.hasNoText(0)) {
          continue;
        }
        try {
          dbHarness.insertValues(geneSymbol, dataRow);
        } catch (SQLException e) {
          throw new RuntimeException("Error processing row " + (i+1), e);
        }
      }
    }
    addImportHistory(workbook);
  }

  static class DbHarness implements AutoCloseable {
    private Connection conn;
    private PreparedStatement insertPhenotype;
    private PreparedStatement insertFunction;
    private PreparedStatement lookupDiplotypes;
    private PreparedStatement lookupDiplotypesByScore;
    private PreparedStatement insertDiplotype;
    private Map<String, Integer> phenotypeCache = new HashMap<>();
    private boolean useScoreLookup = false;
    private Set<String> loadedDiplotypes = new HashSet<>();

    DbHarness() throws SQLException {
      this.conn = ConnectionFactory.newConnection();
      this.insertPhenotype = conn.prepareStatement("insert into gene_phenotype(genesymbol, phenotype) values (?, ?) returning id");
      this.insertFunction = conn.prepareStatement("insert into phenotype_function(phenotypeid, functionkey, function1, function2, activityscore1, activityscore2, totalactivityscore, description) values (?, ?::jsonb, ?, ?, ?, ?, ?, ?) returning id");
      this.lookupDiplotypes = conn.prepareStatement("select a1.name, a2.name " +
          "from gene_phenotype g join phenotype_function pf on g.id = pf.phenotypeid " +
          "                      join allele a1 on g.genesymbol = a1.genesymbol and a1.clinicalfunctionalstatus=pf.function1 " +
          "                      join allele a2 on g.genesymbol = a2.genesymbol and a2.clinicalfunctionalstatus=pf.function2 " +
          "where pf.id=? order by a1.name, a2.name");
      this.lookupDiplotypesByScore = conn.prepareStatement("select a1.name, a2.name " +
          "from gene_phenotype g join phenotype_function pf on g.id = pf.phenotypeid " +
          "                      join allele a1 on g.genesymbol = a1.genesymbol and a1.activityscore=pf.activityscore1 " +
          "                      join allele a2 on g.genesymbol = a2.genesymbol and a2.activityscore=pf.activityscore2 " +
          "where pf.id=? order by a1.name, a2.name");
      this.insertDiplotype = conn.prepareStatement("insert into phenotype_diplotype(functionphenotypeid, diplotype, diplotypekey) values (?, ?, ?::jsonb)");
    }

    void insertValues(String geneSymbol, RowWrapper row) throws SQLException {
      String a1Fn = row.getText(COL_A1_FN);
      String a2Fn = row.getText(COL_A2_FN);
      String a1Score = normalizeScore(row.getNullableText(COL_A1_SCORE));
      String a2Score = normalizeScore(row.getNullableText(COL_A2_SCORE));
      String totalScore = normalizeScore(row.getNullableText(COL_TOTAL_SCORE));
      String description = row.getNullableText(COL_DESC);
      int phenoId = lookupPhenotype(geneSymbol, row.getText(COL_PHENO));

      this.useScoreLookup = (a1Score != null);

      this.insertFunction.setInt(1, phenoId);
      if (useScoreLookup) {
        this.insertFunction.setString(2, makeFunctionKey(null, null, a1Score, a2Score));
      } else {
        this.insertFunction.setString(2, makeFunctionKey(a1Fn, a2Fn, null, null));
      }
      this.insertFunction.setString(3, a1Fn);
      this.insertFunction.setString(4, a2Fn);
      if (a1Score != null) {
        this.insertFunction.setString(5, a1Score);
      } else {
        this.insertFunction.setNull(5, Types.VARCHAR);
      }
      if (a2Score != null) {
        this.insertFunction.setString(6, a2Score);
      } else {
        this.insertFunction.setNull(6, Types.VARCHAR);
      }
      if (totalScore != null) {
        this.insertFunction.setString(7, totalScore);
      } else {
        this.insertFunction.setNull(7, Types.VARCHAR);
      }
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

    int lookupPhenotype(String gene, String phenotype) throws SQLException {
      String normalizedPhenotype = phenotype.replaceAll(gene + "\\s", "");

      if (phenotypeCache.containsKey(normalizedPhenotype)) {
        return phenotypeCache.get(normalizedPhenotype);
      } else {
        this.insertPhenotype.setString(1, gene);
        this.insertPhenotype.setString(2, normalizedPhenotype);

        try (ResultSet rs = this.insertPhenotype.executeQuery()) {
          if (rs.next()) {
            int phenoId = rs.getInt(1);
            this.phenotypeCache.put(normalizedPhenotype, phenoId);
            return phenoId;
          } else {
            throw new RuntimeException("Couldn't insert phenotype");
          }
        }
      }
    }

    private static String normalizeScore(String score) {
      if (score == null) {
        return null;
      }
      if (score.equals("0.0")) {
        return "0";
      } else {
        return score.toLowerCase();
      }
    }

    @Override
    public void close() throws Exception {
      if (this.conn != null) {
        this.conn.close();
      }
    }
  }
}
