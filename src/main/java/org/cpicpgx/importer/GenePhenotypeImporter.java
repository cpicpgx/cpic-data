package org.cpicpgx.importer;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
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
    private Map<String, Integer> phenotypeCache = new HashMap<>();

    DbHarness() throws SQLException {
      this.conn = ConnectionFactory.newConnection();
      this.insertPhenotype = conn.prepareStatement("insert into gene_phenotype(genesymbol, phenotype) values (?, ?) returning id");
      this.insertFunction = conn.prepareStatement("insert into phenotype_function(phenotypeid, functionkey, function1, function2, activityscore1, activityscore2, totalactivityscore) values (?, ?::jsonb, ?, ?, ?, ?, ?)");
    }

    void insertValues(String geneSymbol, RowWrapper row) throws SQLException {
      String a1Fn = row.getText(COL_A1_FN);
      String a2Fn = row.getText(COL_A2_FN);
      String a1Score = row.getNullableText(COL_A1_SCORE);
      String a2Score = row.getNullableText(COL_A2_SCORE);
      String totalScore = row.getNullableText(COL_TOTAL_SCORE);
      int phenoId = lookupPhenotype(geneSymbol, row.getText(COL_PHENO));

      String fnKey;
      if (a1Fn.equals(a2Fn)) {
        fnKey = "{\""+a1Fn+"\": 2}";
      } else {
        fnKey = "{\""+a1Fn+"\": 1, \""+a2Fn+"\": 1}";
      }

      this.insertFunction.setInt(1, phenoId);
      this.insertFunction.setString(2, fnKey);
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
      this.insertFunction.executeUpdate();
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

    @Override
    public void close() throws Exception {
      if (this.conn != null) {
        this.conn.close();
      }
    }
  }
}
