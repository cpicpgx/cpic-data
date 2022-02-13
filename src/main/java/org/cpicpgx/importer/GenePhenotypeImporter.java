package org.cpicpgx.importer;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sawano.java.text.AlphanumericComparator;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses gene phenotype data from workbooks meant to mirror Table 1 information from guidelines
 *
 * @author Ryan Whaley
 */
public class GenePhenotypeImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FILE_SUFFIX = "_phenotypes.xlsx";
  private static final String[] sf_deleteStatements = new String[]{
      "delete from gene_result_diplotype",
      "delete from gene_result_lookup",
      "delete from gene_result"
  };
  private static final Pattern GENE_PATTERN = Pattern.compile("Gene:\\s+([\\w-]+)");

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

    try (PhenoDbHarness dbHarness = new PhenoDbHarness(geneSymbol)) {
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

  static class PhenoDbHarness extends DbHarness {
    private final static List<String> sf_singleAlleleGeneList = ImmutableList.of("chrX", "chrY", "chrM");
    private final PreparedStatement insertPhenotype;
    private final PreparedStatement insertLookup;
    private final PreparedStatement lookupDiplotypes;
    private final PreparedStatement lookupDiplotypesByScore;
    private final PreparedStatement insertDiplotype;
    private final PreparedStatement lookupAllelesByFn;
    private final PreparedStatement validateFn;
    private final String geneSymbol;
    private final Map<String, Integer> phenotypeCache = new HashMap<>();
    private LookupMethod lookupMethod;
    private boolean allowSingleAlleles = false;
    private final Set<String> loadedDiplotypes = new HashSet<>();

    PhenoDbHarness(String geneSymbol) throws SQLException {
      super(FileType.GENE_PHENOTYPE);
      this.geneSymbol = geneSymbol;
      //language=PostgreSQL
      this.insertPhenotype = prepare("insert into gene_result(genesymbol, result, activityScore) values (?, ?, ?) returning id");
      //language=PostgreSQL
      this.insertLookup = prepare("insert into gene_result_lookup(phenotypeid, lookupKey, function1, function2, activityvalue1, activityvalue2, totalactivityscore, description) values (?, ?::jsonb, ?, ?, ?, ?, ?, ?) returning id");
      //language=PostgreSQL
      this.lookupDiplotypes = prepare("select a1.name, a2.name " +
          "from gene_result g join gene_result_lookup pf on g.id = pf.phenotypeid " +
          "                      join allele a1 on g.genesymbol = a1.genesymbol and a1.clinicalfunctionalstatus=pf.function1 " +
          "                      join allele a2 on g.genesymbol = a2.genesymbol and a2.clinicalfunctionalstatus=pf.function2 " +
          "where pf.id=? order by a1.name, a2.name");
      //language=PostgreSQL
      this.lookupDiplotypesByScore = prepare("select a1.name, a2.name " +
          "from gene_result g join gene_result_lookup pf on g.id = pf.phenotypeid " +
          "                      join allele a1 on g.genesymbol = a1.genesymbol and a1.activityvalue=pf.activityvalue1 " +
          "                      join allele a2 on g.genesymbol = a2.genesymbol and a2.activityvalue=pf.activityvalue2 " +
          "where pf.id=? order by a1.name, a2.name");
      //language=PostgreSQL
      this.lookupAllelesByFn = prepare("select a.name from gene_result g join gene_result_lookup pf on g.id = pf.phenotypeid " +
          "join allele a on g.genesymbol=a.genesymbol and a.clinicalFunctionalStatus=pf.function1 " +
          "where pf.id=?");
      //language=PostgreSQL
      this.insertDiplotype = prepare("insert into gene_result_diplotype(functionphenotypeid, diplotype, diplotypekey) values (?, ?, ?::jsonb)");
      //language=PostgreSQL
      this.validateFn = prepare("select count(*) from allele where genesymbol=? and clinicalfunctionalstatus=?");

      //language=PostgreSQL
      PreparedStatement lookupGene = prepare("select lookupMethod, chr from gene where symbol=?");
      lookupGene.setString(1, geneSymbol);
      try (ResultSet rs = lookupGene.executeQuery()) {
        while (rs.next()) {
          this.lookupMethod = LookupMethod.valueOf(rs.getString(1));
          if (sf_singleAlleleGeneList.contains(rs.getString(2))) {
            this.allowSingleAlleles = true;
          }
        }
      }
    }

    String validateFunction(String rawFn) throws Exception {
      String fn = normalizeGeneText(this.geneSymbol, rawFn);
      if (StringUtils.isBlank(fn)) {
        throw new NotFoundException("No function specified");
      }

      this.validateFn.clearParameters();
      this.validateFn.setString(1, this.geneSymbol);
      this.validateFn.setString(2, fn);
      try (ResultSet rs = this.validateFn.executeQuery()) {
        if (rs.next()) {
          int count = rs.getInt(1);
          if (count == 0 && !(allowSingleAlleles && Constants.isUnspecified(fn))) {
            throw new NotFoundException(String.format("No %s alleles have been assigned function [%s]", this.geneSymbol, fn));
          }
          return fn;
        } else {
          throw new NotFoundException("No result found for allele function, unexpected result");
        }
      }
    }

    void insertValues(RowWrapper row) throws Exception {
      String a1Fn = validateFunction(row.getText(COL_A1_FN));
      String a2Fn = validateFunction(row.getText(COL_A2_FN));
      String a1Value = Optional.ofNullable(normalizeScore(row.getNullableText(COL_A1_VALUE))).orElse(Constants.NA);
      String a2Value = Optional.ofNullable(normalizeScore(row.getNullableText(COL_A2_VALUE))).orElse(Constants.NA);
      String totalScore = Optional.ofNullable(normalizeScore(row.getNullableText(COL_TOTAL_SCORE))).orElse(Constants.NA);
      String description = row.getNullableText(COL_DESC);
      int phenoId = lookupPhenotype(row.getText(COL_PHENO), totalScore);

      if (description == null) {
        sf_logger.warn("Missing description for {}", row.getText(COL_PHENO));
      }

      validateScoreData(a1Value, a2Value, totalScore);

      this.insertLookup.setInt(1, phenoId);
      if (this.lookupMethod == LookupMethod.ACTIVITY_SCORE) {
        this.insertLookup.setString(2, makeLookupKey(null, null, a1Value, a2Value));
      } else {
        this.insertLookup.setString(2, makeLookupKey(a1Fn, a2Fn, null, null));
      }
      this.insertLookup.setString(3, a1Fn);
      this.insertLookup.setString(4, a2Fn);
      this.insertLookup.setString(5, a1Value);
      this.insertLookup.setString(6, a2Value);
      this.insertLookup.setString(7, totalScore);
      if (description != null) {
        this.insertLookup.setString(8, description);
      } else {
        this.insertLookup.setNull(8, Types.VARCHAR);
      }

      try (ResultSet rs = this.insertLookup.executeQuery()) {
        if (rs.next()) {
          int fnId = rs.getInt(1);
          if (allowSingleAlleles && !a1Fn.equalsIgnoreCase(Constants.NA) && a2Fn.equalsIgnoreCase(Constants.NA)) {
            insertSingleAllele(fnId);
          } else {
            insertDiplotypes(fnId);
          }
        } else {
          throw new RuntimeException("Couldn't insert function");
        }
      }
    }

    /**
     * Generates gene_result_diplotype records based off of data about results in the gene_result_lookup table
     * @param functionId the primary key ID for a phenotype
     * @throws SQLException can occur when inserting into the DB
     */
    void insertDiplotypes(int functionId) throws SQLException {
      PreparedStatement lookup = this.lookupMethod == LookupMethod.ACTIVITY_SCORE ? this.lookupDiplotypesByScore : this.lookupDiplotypes;

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

          JsonObject geneKey = new JsonObject();
          geneKey.add(geneSymbol, diplotypeKey);

          this.insertDiplotype.setInt(1, functionId);
          this.insertDiplotype.setString(2, diplotypeText);
          this.insertDiplotype.setString(3, geneKey.toString());
          this.insertDiplotype.executeUpdate();
          this.loadedDiplotypes.add(diplotypeText);
        }
      }
    }

    void insertSingleAllele(int functionId) throws SQLException {
      lookupAllelesByFn.setInt(1, functionId);
      ResultSet rs = lookupAllelesByFn.executeQuery();

      while (rs.next()) {
        String rawAllele = rs.getString(1);

        JsonObject diplotypeKey = new JsonObject();
        diplotypeKey.addProperty(rawAllele, 1);
        JsonObject geneKey = new JsonObject();
        geneKey.add(geneSymbol, diplotypeKey);

        this.insertDiplotype.setInt(1, functionId);
        this.insertDiplotype.setString(2, rawAllele);
        this.insertDiplotype.setString(3, geneKey.toString());
        this.insertDiplotype.executeUpdate();
      }
    }

    int lookupPhenotype(String phenotype, String score) throws SQLException {
      String normalizedPhenotype = normalizeGeneText(this.geneSymbol, phenotype);
      String normalizedScore = normalizeScore(score);
      String lookupKey;
      if (this.lookupMethod == LookupMethod.ACTIVITY_SCORE) {
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
      if (this.lookupMethod != LookupMethod.ACTIVITY_SCORE) {
        if (!Constants.isUnspecified(score1) || !Constants.isUnspecified(score2) || !Constants.isUnspecified(total)) {
          throw new RuntimeException("Score data provided for non-score gene");
        } else {
          return;
        }
      }

      // if score data is missing throw an exception
      if (score1 == null || score2 == null || total == null) {
        throw new RuntimeException("Score data is missing");
      }

      // if "n/a" values are present
      if (score1.equalsIgnoreCase(Constants.NA) || score2.equalsIgnoreCase(Constants.NA)) {
        // the total must be "n/a"
        if (!total.equalsIgnoreCase(Constants.NA)){
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
  }
}
