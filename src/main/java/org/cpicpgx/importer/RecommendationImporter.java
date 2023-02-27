package org.cpicpgx.importer;

import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.cpicpgx.workbook.AbstractWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importer class for recommendation Excel workbooks.
 * 
 * This expects the workbook to follow these rules
 * 
 * <ol>
 *   <li>The name of the file must start with the drug name followed by a space then "recommendation""</li>
 *   <li>Each sheet is a different "population" with a title in the form "population {name-of-population}" where only {name-of-population} is stored</li>
 *   <li>File names must all end in ".xlsx"</li>
 *   <li>First row is a header</li>
 *   <li>n is the number of genes used in this recommendation workbook</li>
 *   <li>First n*2 columns are column pairs of gene phenotype (GENE Phenotype) and gene activity score (GENE Activity Score)</li>
 *   <li>THe next n columns are for Implications (GENE Implications for Phenotypic Measures)</li>
 *   <li>After gene column(s) are three columns: implication, recommendation, strength (header text doesn't matter but order does)</li>
 *   <li>If only on one gene, the implication column does not need to mention the gene. If more than one gene, the implication column headers must start with the gene symbol</li>
 * </ol>
 *
 * @author Ryan Whaley
 */
public class RecommendationImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FILE_NAME_SUFFIX = " recommendation.xlsx";
  private static final String[] sf_deleteStatements = new String[]{
      "delete from recommendation",
      "delete from file_note where type='" + FileType.RECOMMENDATION.name() + "'",
      "delete from change_log where type='" + FileType.RECOMMENDATION.name() + "'"
  };
  private static final Pattern PHENO_PATTERN = Pattern.compile("([\\w-]+)\\s+[Pp]henotype");
  private static final Pattern IMPL_PATTERN = Pattern.compile("([\\w-]+)?\\s*[Ii]mplication.*");
  private static final Pattern AS_PATTERN = Pattern.compile("([\\w-]+)?\\s*[Aa]ctivity [Ss]core.*");
  private static final Pattern ALLELE_PATTERN = Pattern.compile("([\\w-]+)\\s+[Aa]llele.*");

  public static void main(String[] args) {
    rebuild(new RecommendationImporter(), args);
  }
  
  public RecommendationImporter() {}

  @Override
  public FileType getFileType() {
    return FileType.RECOMMENDATION;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return FILE_NAME_SUFFIX;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    String drugText = workbook.getFileName().replaceAll(FILE_NAME_SUFFIX, "").toLowerCase();
    // some recommendation file names have more than one drug name in them, load the same data for each drug
    String[] drugNames = drugText.split("_");

    for (String drugName : drugNames) {
      try (RecDbHarness dbHarness = new RecDbHarness(drugName)) {
        for (Iterator<Sheet> sheetIterator = workbook.getSheetIterator(); sheetIterator.hasNext(); ) {
          Sheet sheet = sheetIterator.next();
          workbook.currentSheetIs(sheet.getSheetName());

          if (sheet.getSheetName().equals(AbstractWorkbook.HISTORY_SHEET_NAME)) {
            for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
              RowWrapper row = workbook.getRow(i);
              if (row.hasNoText(0) ^ row.hasNoText(1)) {
                throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
              } else if (row.hasNoText(0)) continue;

              java.util.Date date = row.getDate(0);
              String note = row.getNullableText(1);
              dbHarness.insertChange(date, note);
            }
            continue;
          }

          String populationName = sheet.getSheetName().replaceAll("^population\\s+", "");
          if (StringUtils.isBlank(populationName)) {
            populationName = "general";
          }

          RowWrapper headerRow = workbook.getRow(0);

          // caching column indexes for different types of data
          Map<String, Integer> phenotypeIdxMap = new HashMap<>();
          Map<String, Integer> implIdxMap = new HashMap<>();
          Map<String, Integer> asIdxMap = new HashMap<>();
          Map<String, Integer> alleleIdxMap = new HashMap<>();
          int idxRecommendation = -1;
          int idxClassification = -1;
          int idxComments = -1;
          int idxAltDrug = -1;
          int idxDosingInfo = -1;
          int idxOtherPrescribing = -1;

          for (int j = 0; j < headerRow.getLastCellNum(); j++) {
            String cellText = headerRow.getNullableText(j);
            if (cellText == null) continue;

            Matcher implMatch = IMPL_PATTERN.matcher(cellText);
            Matcher asMatch = AS_PATTERN.matcher(cellText);
            Matcher phenoMatch = PHENO_PATTERN.matcher(cellText);
            Matcher alleleMatch = ALLELE_PATTERN.matcher(cellText);

            // figuring out the index of each type of column
            if (implMatch.matches()) {
              if (implMatch.group(1) != null) {
                implIdxMap.put(implMatch.group(1), j);
              } else if (dbHarness.getGenes().size() == 1) {
                implIdxMap.put(dbHarness.getGenes().get(0), j);
              } else {
                throw new RuntimeException("Unexpected implication column setup");
              }
            } else if (asMatch.matches()) {
              if (asMatch.group(1) != null) {
                asIdxMap.put(asMatch.group(1), j);
              } else if (dbHarness.getGenes().size() == 1) {
                asIdxMap.put(dbHarness.getGenes().get(0), j);
              } else {
                throw new RuntimeException("Unexpected activity score setup for " + drugName);
              }
            } else if (phenoMatch.matches()) {
              phenotypeIdxMap.put(phenoMatch.group(1), j);
            } else if (alleleMatch.matches()) {
              alleleIdxMap.put(alleleMatch.group(1), j);
            } else if (cellText.equalsIgnoreCase("Therapeutic Recommendation")) {
              idxRecommendation = j;
            } else if (cellText.toLowerCase().contains("classification of recommendation")) {
              idxClassification = j;
            } else if (cellText.toLowerCase().contains("comments")) {
              idxComments = j;
            } else if (cellText.toLowerCase().contains("dosing information?")) {
              idxDosingInfo = j;
            } else if (cellText.toLowerCase().contains("alternate drug?")) {
              idxAltDrug = j;
            } else if (cellText.toLowerCase().contains("other prescribing info?")) {
              idxOtherPrescribing = j;
            } else {
              throw new RuntimeException("Unrecognized column: " + cellText);
            }
          }

          Set<String> observedGenes = new HashSet<>();
          observedGenes.addAll(phenotypeIdxMap.keySet());
          observedGenes.addAll(alleleIdxMap.keySet());
          observedGenes.addAll(asIdxMap.keySet());
          if (!CollectionUtils.isEqualCollection(observedGenes, dbHarness.getGenes())) {
            throw new RuntimeException("Observed genes " + String.join(";", phenotypeIdxMap.keySet()) + " does not match expected genes " + String.join(";", dbHarness.getGenes()));
          }

          for (String gene : dbHarness.getGenes()) {
            switch (dbHarness.geneLookupCache.get(gene)) {
              case ACTIVITY_SCORE:
                if (asIdxMap.get(gene) == null) {
                  throw new RuntimeException("Activity score column missing for " + gene);
                }
                if (phenotypeIdxMap.get(gene) == null) {
                  throw new RuntimeException("Phenotype column missing for " + gene);
                }
                break;
              case PHENOTYPE:
                if (phenotypeIdxMap.get(gene) == null) {
                  throw new RuntimeException("Phenotype column missing for " + gene);
                }
                break;
              case ALLELE_STATUS:
                if (alleleIdxMap.get(gene) == null) {
                  throw new RuntimeException("Allele status column missing for " + gene);
                }
                break;
              default:
                throw new RuntimeException("Unsupported lookup method for " + gene);
            }
          }

          for (int k = 1; k <= workbook.currentSheet.getLastRowNum(); k++) {
            try {
              RowWrapper dataRow = workbook.getRow(k);
              if (dataRow.hasNoText(0)) continue;

              Map<String,String> phenotype = new HashMap<>();
              Map<String,String> implication = new HashMap<>();
              Map<String,String> activityScore = new HashMap<>();
              Map<String,String> alleleStatus = new HashMap<>();
              Map<String,String> lookupKey = new HashMap<>();

              for (String gene : phenotypeIdxMap.keySet()) {
                String normalizedPheno = normalizeGeneText(gene, dataRow.getText(phenotypeIdxMap.get(gene)));
                if (normalizedPheno == null) {
                  throw new RuntimeException("No phenotype found");
                }
                String validPhenotype = dbHarness.validPhenotype(gene, normalizedPheno);
                phenotype.put(gene, validPhenotype);
              }

              for (String gene : dbHarness.getGenes()) {
                switch (dbHarness.geneLookupCache.get(gene)) {
                  case ACTIVITY_SCORE:
                    lookupKey.put(gene, normalizeScore(normalizeGeneText(gene, dataRow.getText(asIdxMap.get(gene)))));
                    break;
                  case ALLELE_STATUS:
                    lookupKey.put(gene, normalizeGeneText(gene, dataRow.getText(alleleIdxMap.get(gene))));
                    break;
                  case PHENOTYPE:
                    lookupKey.put(gene, phenotype.get(gene));
                    break;
                  default:
                    throw new RuntimeException("Unsupported lookup method for " + gene);
                }
              }

              for (String gene : implIdxMap.keySet()) {
                implication.put(gene, dataRow.getText(implIdxMap.get(gene)));
              }
              for (String gene : asIdxMap.keySet()) {
                activityScore.put(gene, normalizeScore(dataRow.getText(asIdxMap.get(gene))));
              }
              for (String gene : alleleIdxMap.keySet()) {
                alleleStatus.put(gene, dataRow.getText(alleleIdxMap.get(gene)));
              }

              // Validate not all "No Result" in multi-gene rec
              if (
                  phenotype.values().stream().allMatch((v) -> v.equals(Constants.NO_RESULT))
                  && alleleStatus.values().stream().allMatch((v) -> v.equals(Constants.NO_RESULT))
              ) {
                sf_logger.warn("Row {} contains all No Result", k + 1);
              }
              // Validate no use of "No Result in single-gene rec
              if ((alleleStatus.size() + phenotype.size()) == 1 && phenotype.containsValue(Constants.NO_RESULT)) {
                sf_logger.warn("Single-gene recommendations should not use 'No Result'");
              }

              boolean altDrug = idxAltDrug >= 0 && dataRow.getNullableText(idxAltDrug) != null && dataRow.getNullableText(idxAltDrug).equals("yes");
              boolean dosingInfo = idxDosingInfo >= 0 && dataRow.getNullableText(idxDosingInfo) != null && dataRow.getNullableText(idxDosingInfo).equals("yes");
              boolean otherPrescribing = idxOtherPrescribing >= 0 && dataRow.getNullableText(idxOtherPrescribing) != null && dataRow.getNullableText(idxOtherPrescribing).equals("yes");

              dbHarness.insert(
                  phenotype,
                  implication,
                  dataRow.getText(idxRecommendation),
                  normalizeClassification(dataRow.getText(idxClassification)),
                  dataRow.getNullableText(idxComments),
                  activityScore,
                  populationName,
                  lookupKey,
                  alleleStatus,
                  altDrug,
                  dosingInfo,
                  otherPrescribing
              );
            } catch (RuntimeException ex) {
              throw new RuntimeException("Error reading row " + (k + 1), ex);
            }
          }
        }
      }
    }
  }

  @Nonnull
  private String normalizeClassification(@Nonnull String classification) {
    if (classification.equals(Constants.NA)) {
      return Constants.NA;
    } else {
      return WordUtils.capitalize(classification);
    }
  }

  private static class RecDbHarness extends DbHarness {
    private final PreparedStatement insertStmt;
    private final String drugId;
    private final Long guidelineId;
    private final Map<String, LookupMethod> geneLookupCache = new HashMap<>();
    private final Gson gson = new Gson();

    RecDbHarness(String drugName) throws Exception {
      super(FileType.RECOMMENDATION);
      //language=PostgreSQL
      this.insertStmt = prepare("insert into recommendation(guidelineid, drugid, implications, drugRecommendation, classification, phenotypes, comments, activityScore, population, lookupKey, alleleStatus, dosinginformation, alternatedrugavailable, otherprescribingguidance) values (?, ?, ?::jsonb, ?, ? , ?::jsonb, ?, ?::jsonb, ?, ?::jsonb, ?::jsonb, ?, ?, ?)");

      //language=PostgreSQL
      PreparedStatement drugLookupStmt = prepare("select drugid, guidelineid from drug where regexp_replace(name, '[ /-_]+','')=regexp_replace(?, '[ /-_]+','') and guidelineid is not null");
      drugLookupStmt.setString(1, drugName);
      ResultSet rs = drugLookupStmt.executeQuery();
      if (rs.next()) {
        drugId = rs.getString(1);
        guidelineId = rs.getLong(2);
      } else {
        throw new NotFoundException("Couldn't find drug with guideline for: " + drugName);
      }
      if (rs.next()) {
        throw new RuntimeException("More than one guideline found for: " + drugName);
      }

      //language=PostgreSQL
      PreparedStatement stmt = prepare("select genesymbol, g.lookupmethod from pair p join drug d on p.drugid = d.drugid join gene g on p.genesymbol = g.symbol where  regexp_replace(d.name, '[ /-_]+','')=regexp_replace(?, '[ /-_]+','') and p.usedForRecommendation = true");
      stmt.setString(1, drugName);
      try (ResultSet stmtRs = stmt.executeQuery()) {
        while (stmtRs.next()) {
          geneLookupCache.put(stmtRs.getString(1), LookupMethod.valueOf(stmtRs.getString(2)));
          sf_logger.debug("Expecting gene data for: " + String.join(", ", geneLookupCache.keySet()));
        }
      }
      if (geneLookupCache.size() == 0) {
        throw new NotFoundException("No gene found in a pair used for lookup for " + drugName);
      }

      sf_logger.debug("Drug: {}; Drug ID: {}; Guideline ID: {}", drugName, drugId, guidelineId);
    }

    List<String> getGenes() {
      return new ArrayList<>(geneLookupCache.keySet());
    }

    void insert(Map<String,String> phenotype, Map<String,String> implication, String recommendation,
                String classification, String comments, Map<String,String> activityScore, String population,
                Map<String,String> lookupKey, Map<String,String> alleleStatus, boolean dosingInfo, boolean altDrug,
                boolean otherPrescribing
    ) {
      try {
        this.insertStmt.clearParameters();

        // Validate that activity score genes have activity score values filled in
        for (String gene : activityScore.keySet()) {
          LookupMethod lookupMethod = geneLookupCache.get(gene);
          if (lookupMethod == LookupMethod.ACTIVITY_SCORE && activityScore.get(gene).equals(Constants.NA)) {
            String genePhenotype = phenotype.get(gene);
            if (!genePhenotype.equals("Indeterminate") && !genePhenotype.equals(Constants.NO_RESULT)) {
              sf_logger.warn("{} is an activity gene but has a missing activity value for {} {}", gene, population, gson.toJson(phenotype));
            }
          }
        }

        this.insertStmt.setLong(1, guidelineId);
        this.insertStmt.setString(2, drugId);
        this.insertStmt.setString(3, gson.toJson(implication));
        if (StringUtils.isNotBlank(recommendation)) {
          this.insertStmt.setString(4, recommendation);
        } else {
          this.insertStmt.setNull(4, Types.VARCHAR);
        }
        if (StringUtils.isNotBlank(classification)) {
          this.insertStmt.setString(5, classification);
        } else {
          this.insertStmt.setNull(5, Types.VARCHAR);
        }
        this.insertStmt.setObject(6, gson.toJson(phenotype));

        if (comments == null) {
          this.insertStmt.setNull(7, Types.VARCHAR);
        } else {
          this.insertStmt.setString(7, comments);
        }
        this.insertStmt.setString(8, gson.toJson(activityScore));
        this.insertStmt.setString(9, population);
        this.insertStmt.setString(10, gson.toJson(lookupKey));
        this.insertStmt.setString(11, gson.toJson(alleleStatus));

        this.insertStmt.setBoolean(12, dosingInfo);
        this.insertStmt.setBoolean(13, altDrug);
        this.insertStmt.setBoolean(14, otherPrescribing);

        this.insertStmt.executeUpdate();
        
      } catch (Exception e) {
        throw new RuntimeException("Error inserting record", e);
      }
    }

    /**
     * Insert a change event into the history table.
     * @param date the date of the change, required
     * @param note the text note to explain the change
     * @throws SQLException can occur from bad database transaction
     */
    void insertChange(java.util.Date date, String note) throws SQLException {
      writeChangeLog(drugId, date, note);
    }
  }
}
