package org.cpicpgx.importer;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.*;
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
 * </ol>
 *
 * @author Ryan Whaley
 */
public class RecommendationImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String FILE_NAME_SUFFIX = " recommendation.xlsx";
  private static final String[] sf_deleteStatements = new String[]{
      "delete from recommendation"
  };
  private static final String DEFAULT_DIRECTORY = "recommendation_tables";
  private static final Pattern PHENO_PATTERN = Pattern.compile("([\\w-]+)\\s+Phenotype");
  private static final Pattern IMPL_PATTERN = Pattern.compile("([\\w-]+)\\s+Implication.*");
  private static final Pattern AS_PATTERN = Pattern.compile("([\\w-]+)\\s+Activity Score.*");

  public static void main(String[] args) {
    rebuild(new RecommendationImporter(), args);
  }
  
  public RecommendationImporter() {}

  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.RECOMMENDATIONS;
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
    try (DbHarness dbHarness = new DbHarness()) {
      String drugText = workbook.getFileName().replaceAll(FILE_NAME_SUFFIX, "").toLowerCase();
      // some recommendation file names have more than one drug name in them, laod the same data for each drug
      String[] drugNames = drugText.split("_");

      for (String drugName : drugNames) {
        String drugId = dbHarness.lookupDrug(drugName);
        long guidelineId = dbHarness.lookupGuideline(drugName);

        sf_logger.debug("Drug: {} {}", drugName, drugId);
        sf_logger.debug("Guideline: {}", guidelineId);
        for (Iterator<Sheet> sheetIterator = workbook.getSheetIterator(); sheetIterator.hasNext(); ) {
          Sheet sheet = sheetIterator.next();
          workbook.currentSheetIs(sheet.getSheetName());
          if (!sheet.getSheetName().startsWith("population")) {
            throw new RuntimeException("Improper sheet name: " + sheet.getSheetName());
          }

          String populationName = sheet.getSheetName().replaceAll("^population\\s+", "");
          if (StringUtils.isBlank(populationName)) {
            populationName = "general";
          }

          RowWrapper headerRow = workbook.getRow(0);
          Map<String, Integer> phenotypeIdxMap = getPhenotypeIndexMap(headerRow);
          Map<String, Integer> implIdxMap = new HashMap<>();
          Map<String, Integer> asIdxMap = new HashMap<>();
          int idxRecommendation = -1;
          int idxClassification = -1;
          int idxComments = -1;

          for (int j = 0; j < headerRow.getLastCellNum(); j++) {
            String cellText = headerRow.getNullableText(j);
            if (cellText == null) continue;

            // figuring out the index of each type of column
            Matcher implMatch = IMPL_PATTERN.matcher(cellText);
            Matcher asMatch = AS_PATTERN.matcher(cellText);
            if (phenotypeIdxMap.size() > 1 && implMatch.matches()) {
              implIdxMap.put(implMatch.group(1), j);
            } else if (cellText.toLowerCase().startsWith("implication")) {
              implIdxMap.put(phenotypeIdxMap.keySet().iterator().next(), j);
            } else if (phenotypeIdxMap.size() > 1 && asMatch.matches()) {
              asIdxMap.put(asMatch.group(1), j);
            } else if (cellText.toLowerCase().contains("activity score")) {
              asIdxMap.put(phenotypeIdxMap.keySet().iterator().next(), j);
            } else if (cellText.equalsIgnoreCase("Therapeutic Recommendation")) {
              idxRecommendation = j;
            } else if (cellText.toLowerCase().contains("classification of recommendation")) {
              idxClassification = j;
            } else if (cellText.toLowerCase().contains("comments")) {
              idxComments = j;
            }
          }

          for (int k = 1; k <= workbook.currentSheet.getLastRowNum(); k++) {
            try {
              RowWrapper dataRow = workbook.getRow(k);
              if (dataRow.hasNoText(0)) continue;

              JsonObject phenotype = new JsonObject();
              JsonObject implication = new JsonObject();
              JsonObject activityScore = new JsonObject();

              for (String gene : phenotypeIdxMap.keySet()) {
                String normalizedPheno = WordUtils.capitalize(StringUtils.strip(dataRow.getText(phenotypeIdxMap.get(gene)).replaceAll("\\s*" + gene + "\\s*", " ")));
                phenotype.addProperty(gene, normalizedPheno);
              }
              for (String gene : implIdxMap.keySet()) {
                implication.addProperty(gene, dataRow.getText(implIdxMap.get(gene)));
              }
              for (String gene : asIdxMap.keySet()) {
                activityScore.addProperty(gene, dataRow.getText(asIdxMap.get(gene)));
              }
              dbHarness.insert(
                  drugName,
                  phenotype,
                  implication,
                  dataRow.getNullableText(idxRecommendation),
                  dataRow.getNullableText(idxClassification),
                  dataRow.getNullableText(idxComments),
                  activityScore,
                  populationName
              );
            } catch (RuntimeException ex) {
              throw new RuntimeException("Error reading row " + (k + 1), ex);
            }
          }
        }
      }
    }
  }

  private Map<String, Integer> getPhenotypeIndexMap(RowWrapper headerRow) {
    Map<String, Integer> phenotypeIdxMap = new HashMap<>();
    for (int j = 0; j < headerRow.getLastCellNum(); j++) {
      String cellText = headerRow.getNullableText(j);
      if (cellText == null) continue;

      Matcher phenoMatch = PHENO_PATTERN.matcher(cellText);
      if (phenoMatch.matches()) {
        phenotypeIdxMap.put(phenoMatch.group(1), j);
      }
    }

    if (phenotypeIdxMap.size() == 0) {
      throw new RuntimeException("Phenotype column not in expected format");
    }
    return phenotypeIdxMap;
  }

  private static class DbHarness implements AutoCloseable {
    private PreparedStatement insertStmt;
    private PreparedStatement drugLookupStmt;
    private PreparedStatement guidelineLookupStmt;
    private List<AutoCloseable> closables = new ArrayList<>();
    
    DbHarness() throws SQLException {
      Connection conn = ConnectionFactory.newConnection();
      this.insertStmt = conn.prepareStatement("insert into recommendation(guidelineid, drugid, implications, drug_recommendation, classification, phenotypes, comments, activity_score, population) values (?, ?, ?::jsonb, ?, ? , ?::jsonb, ?, ?::jsonb, ?)");
      this.drugLookupStmt = conn.prepareStatement(
          "select drugid from drug where name=?", 
          ResultSet.TYPE_SCROLL_INSENSITIVE, 
          ResultSet.CONCUR_READ_ONLY);
      this.guidelineLookupStmt = conn.prepareStatement(
          "select distinct p.guidelineid from pair p join drug d on p.drugid = d.drugid where d.name=? and p.guidelineid is not null", 
          ResultSet.TYPE_SCROLL_INSENSITIVE, 
          ResultSet.CONCUR_READ_ONLY);
      closables.add(this.guidelineLookupStmt);
      closables.add(this.drugLookupStmt);
      closables.add(this.insertStmt);
      closables.add(conn);
    }
    
    void insert(String drug, JsonObject phenotype, JsonObject implication, String recommendation, String classification, String comments, JsonObject activityScore, String population) {
      try {
        String drugId = lookupDrug(drug);
        Long guidelineId = lookupGuideline(drug);
        this.insertStmt.clearParameters();

        this.insertStmt.setLong(1, guidelineId);
        this.insertStmt.setString(2, drugId);
        this.insertStmt.setString(3, implication.toString());
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
        this.insertStmt.setObject(6, phenotype.toString());

        if (comments == null) {
          this.insertStmt.setNull(7, Types.VARCHAR);
        } else {
          this.insertStmt.setString(7, comments);
        }
        this.insertStmt.setString(8, activityScore.toString());
        this.insertStmt.setString(9, population);
        
        this.insertStmt.executeUpdate();
        
      } catch (Exception e) {
        throw new RuntimeException("Error inserting record", e);
      }
    }
    
    private String lookupDrug(String name) throws SQLException, NotFoundException {
      this.drugLookupStmt.clearParameters();
      this.drugLookupStmt.setString(1, name);
      ResultSet rs = this.drugLookupStmt.executeQuery();
      if (rs.first()) {
        return rs.getString(1);
      } else {
        throw new NotFoundException("Couldn't find drug " + name);
      }
    }

    private Long lookupGuideline(String name) throws SQLException, NotFoundException {
      this.guidelineLookupStmt.clearParameters();
      this.guidelineLookupStmt.setString(1, name);
      ResultSet rs = this.guidelineLookupStmt.executeQuery();
      if (rs.first()) {
        if (!rs.isLast()) {
          throw new NotFoundException("Couldn't find just 1 guideline for " + name);
        }
        return rs.getLong(1);
      } else {
        throw new NotFoundException("Couldn't find guideline");
      }
    }

    @Override
    public void close() {
      closables.forEach(c -> {
        try {
          c.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
