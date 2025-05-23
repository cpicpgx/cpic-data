package org.cpicpgx.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.*;
import org.cpicpgx.workbook.AbstractWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cpicpgx.util.Constants.isUnspecified;

/**
 * Class to parse references for functional assignments from a directory of XLSX files.
 *
 * @author Ryan Whaley
 */
public class FunctionReferenceImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern sf_geneLabelPattern = Pattern.compile("GENE:\\s([\\w-]+)");
  private static final Pattern sf_alleleNamePattern = Pattern.compile("^(.+?)([x≥]+(\\d+|N))?$");
  private static final Pattern sf_pmidPattern = Pattern.compile("^\\d+$");
  private static final int COL_IDX_ALLELE = 0;
  private static final int COL_IDX_ACTIVITY = 1;
  private static final int COL_IDX_FUNCTION = 2;
  private static final int COL_IDX_CLINICAL_FUNCTION = 3;
  private static final int COL_IDX_CLINICAL_SUBSTRATE = 4;
  private static final int COL_IDX_PMID = 5;
  private static final int COL_IDX_STRENGTH = 6;
  private static final int COL_IDX_FINDINGS = 7;
  private static final int COL_IDX_COMMENTS = 8;

  private static final String[] sf_deleteStatements = new String[]{
      "delete from change_log where type='" + FileType.ALLELE_FUNCTION_REFERENCE.name() + "'",
      "delete from file_note where type='" + FileType.ALLELE_FUNCTION_REFERENCE.name() + "'"
  };

  public static void main(String[] args) {
    rebuild(new FunctionReferenceImporter(), args);
  }

  public FunctionReferenceImporter() { }
  
  String getFileExtensionToProcess() {
    return Constants.EXCEL_EXTENSION;
  }

  @Override
  public FileType getFileType() {
    return FileType.ALLELE_FUNCTION_REFERENCE;
  }

  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    int rowIdx = 0;

    RowWrapper row = null;
    String geneSymbol = null;
    for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
      row = workbook.getRow(rowIdx);
      String geneLabel = row.getNullableText(0);
      if (geneLabel == null) continue;
      
      Matcher m = sf_geneLabelPattern.matcher(geneLabel);
      if (m.find()) {
        geneSymbol = m.group(1);
        break;
      }
    }
    
    if (geneSymbol == null) {
      throw new NotFoundException("Couldn't find gene symbol");
    }
    
    sf_logger.debug("This sheet is for {}, {}", geneSymbol, row.getNullableText(1));

    List<String> noFunctionAlleles = new ArrayList<>();
    rowIdx += 2; // move down 2 rows and start reading;
    try (FunctionDbHarness dbHarness = new FunctionDbHarness(geneSymbol)) {
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        int readableRow = rowIdx + 1;
        row = workbook.getRow(rowIdx);
        if (row.hasNoText(COL_IDX_ALLELE) || row.getNullableText(COL_IDX_ALLELE).toLowerCase().startsWith("note")) break;
        
        String alleleName = row.getNullableText(COL_IDX_ALLELE);
        String activityValue = normalizeScore(row.getNullableText(COL_IDX_ACTIVITY));
        String functionStatus = TextUtils.normalizeAlleleFunction(row.getNullableText(COL_IDX_FUNCTION));
        String clinicalFunction = TextUtils.normalizeAlleleFunction(row.getNullableText(COL_IDX_CLINICAL_FUNCTION));
        String substrate = row.getNullableText(COL_IDX_CLINICAL_SUBSTRATE);
        String citationClump = row.getNullableText(COL_IDX_PMID, true);
        String strength = row.getNullableText(COL_IDX_STRENGTH);
        String findingsString = row.getNullableText(COL_IDX_FINDINGS);
        String comments = row.getNullableText(COL_IDX_COMMENTS);

        if (clinicalFunction == null) {
          noFunctionAlleles.add(alleleName);
        }

        List<String> citationList = new ArrayList<>();
        if (StringUtils.isNotBlank(citationClump) && !isUnspecified(citationClump)) {
          for (String citation : citationClump.split("[;,.]")) {
            String pmid = StringUtils.strip(citation);
            if (!sf_pmidPattern.matcher(pmid).matches()) {
              throw new RuntimeException("PMID not valid: [" + pmid + "] in row " + readableRow);
            }
            citationList.add(StringUtils.strip(citation));
          }
        }

        JsonElement findings = parseFindingsObject(findingsString);
        if (findings instanceof JsonObject) {
          for (String pmid : ((JsonObject)findings).keySet()) {
            if (!citationList.contains(pmid)) {
              sf_logger.warn("PMID ({}) used in Findings not in PMID field, row {}", pmid, readableRow);
            }
          }
        }

        try {
          dbHarness.insert(
              alleleName,
              activityValue,
              functionStatus,
              clinicalFunction,
              substrate,
              citationList.toArray(new String[0]),
              strength,
              findingsString,
              comments
          );
        }
        catch (RuntimeException ex) {
          throw new RuntimeException("Error on row " + readableRow, ex);
        }
      }

      writeNotes(geneSymbol, workbook.getNotes());

      dbHarness.updateMethods(processMethods(workbook));

      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      processChangeLog(dbHarness, workbook, geneSymbol);
    }

    if (!noFunctionAlleles.isEmpty()) {
      sf_logger.warn("No clinical function assigned to {}", String.join("; ", noFunctionAlleles));
    }
  }

  /**
   * Parses a standard duplication allele name to get the "base" allele's name, or just returns the given string if no
   * duplication syntax found. For example "*2" from "*2xN".
   * @param name a duplication allele name
   * @return the "base" allele name
   */
  static String parseAlleleDefinitionName(@Nonnull String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }

    Matcher m = sf_alleleNamePattern.matcher(name);
    if (m.matches()) {
      return StringUtils.strip(m.group(1));
    } else {
      throw new RuntimeException("Allele name not in expected format");
    }
  }

  private static final String pmidRegex = "(^\\d{7,8}|(?<=\\s)\\d{7,8})";
  private static final Pattern pmidPattern = Pattern.compile(pmidRegex);
  static JsonElement parseFindingsObject(String findings) {
    if (StringUtils.isBlank(findings)) return null;

    JsonObject findingsObject = new JsonObject();

    String[] descriptions = findings.split(pmidRegex);
    Matcher matcher = pmidPattern.matcher(findings);

    int n = 0;
    while (matcher.find()) {
      n += 1;
      String pmid = StringUtils.substring(findings, matcher.start(), matcher.end());
      String description = "";
      if (descriptions.length > n) {
        description = StringUtils.strip(descriptions[n], ":; \n");
      }
      findingsObject.addProperty(pmid, description);
    }
    if (n == 0 && StringUtils.isNotBlank(findings)) {
      return new JsonPrimitive(findings);
    } else if (n > 0) {
      return findingsObject;
    } else {
      return null;
    }
  }


  /**
   * Private class for handling DB interactions
   */
  static class FunctionDbHarness extends DbHarness {
    private final Map<String, Long> alleleNameMap = new HashMap<>();
    private final PreparedStatement insertAlleleStmt;
    private final PreparedStatement insertAlleleDefStmt;
    private final PreparedStatement updateMethodsStmt;
    private final String gene;
    private final LookupMethod geneLookupMethod;
    private boolean inferredFrequency = true;

    FunctionDbHarness(String gene) throws SQLException {
      super(FileType.ALLELE_FUNCTION_REFERENCE);
      this.gene = gene;

      try (PreparedStatement pstmt = prepare("select name, id from allele_definition where geneSymbol=?")) {
        pstmt.setString(1, gene);
        try (ResultSet rs = pstmt.executeQuery()) {
          while (rs.next()) {
            this.alleleNameMap.put(rs.getString(1), rs.getLong(2));
          }
        }
      }

      if (this.alleleNameMap.isEmpty()) {
        throw new RuntimeException("No alleles found for gene: " + gene);
      }

      try (PreparedStatement lstmt = prepare("select lookupmethod from gene where symbol=?")) {
        lstmt.setString(1, gene);
        try (ResultSet rs = lstmt.executeQuery()) {
          if (rs.next()) {
            this.geneLookupMethod = LookupMethod.valueOf(rs.getString(1));
          } else {
            sf_logger.warn("Gene lookup method not found for {}", gene);
            this.geneLookupMethod = null;
          }
        }
      }

      //language=PostgreSQL
      insertAlleleStmt = prepare(
          "insert into allele(geneSymbol, name, definitionId, functionalStatus, activityvalue, " +
              "clinicalfunctionalstatus, clinicalFunctionalSubstrate, citations, strength, findings, functioncomments, inferredfrequency) " +
              "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) on conflict (genesymbol, name) " +
              "do update set functionalStatus=excluded.functionalStatus,activityvalue=excluded.activityvalue," +
              "clinicalfunctionalstatus=excluded.clinicalfunctionalstatus," +
              "clinicalFunctionalSubstrate=excluded.clinicalFunctionalSubstrate,citations=excluded.citations," +
              "strength=excluded.strength,findings=excluded.findings,functioncomments=excluded.functioncomments, inferredfrequency=excluded.inferredfrequency"
      );

      insertAlleleDefStmt = prepare("insert into allele_definition(genesymbol, name) values (?, ?) returning id");

      updateMethodsStmt = prepare("update gene set functionmethods=? where symbol=?");

      // clear the inferredFrequency flag for all alleles for this gene, it will get set properly in insertAlleleStmt
      try (PreparedStatement clearReference = prepare("update allele set inferredfrequency=false where genesymbol=?")) {
        clearReference.setString(1, gene);
        clearReference.executeUpdate();
      }
    }

    private Long lookupAlleleDefinitionId(String alleleName) throws SQLException {
      String alleleDefinitionName = parseAlleleDefinitionName(alleleName);
      if (!this.alleleNameMap.containsKey(alleleDefinitionName)) {
        insertAlleleDefStmt.setString(1, gene);
        insertAlleleDefStmt.setString(2, alleleName);
        ResultSet rs = insertAlleleDefStmt.executeQuery();
        if (rs.next()) {
          long id = rs.getLong(1);
          this.alleleNameMap.put(alleleDefinitionName, id);
          return id;
        }
        throw new RuntimeException("Missing allele definition for " + gene + " " + alleleDefinitionName);
      }
      return this.alleleNameMap.get(alleleDefinitionName);
    }
    
    void insert(
        String allele,
        String activityValue,
        String alleleFunction,
        String clinicalFunction,
        String substrate,
        String[] pmids,
        String strength,
        String findings,
        String comments
    ) throws SQLException {
      Long alleleDefinitionId = lookupAlleleDefinitionId(allele);

      if (StringUtils.isBlank(activityValue) && geneLookupMethod == LookupMethod.ACTIVITY_SCORE) {
        sf_logger.warn("{} is missing an activity score", allele);
      }

      // use the first listed allele for "star" allele genes and for G6PD
      inferredFrequency = inferredFrequency && (allele.startsWith("*") || this.gene.equals("G6PD"));

      this.insertAlleleStmt.clearParameters();
      this.insertAlleleStmt.setString(1, this.gene);
      this.insertAlleleStmt.setString(2, allele);
      this.insertAlleleStmt.setLong(3, alleleDefinitionId);
      this.insertAlleleStmt.setString(4, normalizeFunction(alleleFunction));
      setNullableString(this.insertAlleleStmt, 5, activityValue);
      setNullableString(this.insertAlleleStmt, 6, clinicalFunction);
      setNullableString(this.insertAlleleStmt, 7, substrate);
      this.insertAlleleStmt.setArray(8, createArrayOf(pmids));
      setNullableString(this.insertAlleleStmt, 9, strength);
      setNullableString(this.insertAlleleStmt, 10, findings);
      setNullableString(this.insertAlleleStmt, 11, comments);
      this.insertAlleleStmt.setBoolean(12, inferredFrequency);
      int inserted = this.insertAlleleStmt.executeUpdate();
      if (inserted == 0) {
        throw new RuntimeException("No allele inserted");
      }
      // only the first allele should be the "inferred frequency" so setting this to false after each subsequent insert does that
      inferredFrequency = false;
    }

    void updateMethods(String methods) throws SQLException {
      if (StringUtils.isBlank(methods)) {
        updateMethodsStmt.setNull(1, Types.VARCHAR);
      } else {
        updateMethodsStmt.setString(1, methods);
      }
      updateMethodsStmt.setString(2, this.gene);
      updateMethodsStmt.executeUpdate();
    }

    private String normalizeFunction(String fn) {
      if (fn == null) {
        return null;
      }
      
      return fn
          .replaceAll("Function", "function")
          .replaceAll("unctione", "unction");
    }
  }
}
