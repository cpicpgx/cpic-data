package org.cpicpgx.importer;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse references for functional assignments from a directory of excel files.
 *
 * @author Ryan Whaley
 */
public class FunctionReferenceImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern sf_geneLabelPattern = Pattern.compile("GENE:\\s([\\w-]+)");
  private static final Pattern sf_alleleNamePattern = Pattern.compile("^(.+?)([x≥](\\d+|N))?$");
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
      "delete from allele where geneSymbol not in ('HLA-A','HLA-B')",
      "delete from change_log where type='" + NoteType.FUNCTION_REFERENCE.name() + "'",
      "delete from file_note where type='" + NoteType.FUNCTION_REFERENCE.name() + "'"
  };
  private static final String DEFAULT_DIRECTORY = "allele_functionality_reference";

  public static void main(String[] args) {
    rebuild(new FunctionReferenceImporter(), args);
  }

  public FunctionReferenceImporter() { }
  
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
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
    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        int readableRow = rowIdx + 1;
        row = workbook.getRow(rowIdx);
        if (row.hasNoText(COL_IDX_ALLELE) || row.getNullableText(COL_IDX_ALLELE).toLowerCase().startsWith("note")) break;
        
        String alleleName = row.getNullableText(COL_IDX_ALLELE);
        String activityValue = normalizeScore(row.getNullableText(COL_IDX_ACTIVITY));
        String functionStatus = row.getNullableText(COL_IDX_FUNCTION);
        String clinicalFunction = row.getNullableText(COL_IDX_CLINICAL_FUNCTION);
        String substrate = row.getNullableText(COL_IDX_CLINICAL_SUBSTRATE);
        String citationClump = row.getNullableText(COL_IDX_PMID, true);
        String strength = row.getNullableText(COL_IDX_STRENGTH);
        String findingsString = row.getNullableText(COL_IDX_FINDINGS);
        String comments = row.getNullableText(COL_IDX_COMMENTS);

        if (clinicalFunction == null) {
          noFunctionAlleles.add(alleleName);
        }

        List<String> citationList = new ArrayList<>();
        if (StringUtils.isNotBlank(citationClump)) {
          for (String citation : citationClump.split("[;,\\.]")) {
            String pmid = StringUtils.strip(citation);
            if (!sf_pmidPattern.matcher(pmid).matches()) {
              throw new RuntimeException("PMID not valid: [" + pmid + "] in row " + readableRow);
            }
            citationList.add(StringUtils.strip(citation));
          }
        }

        JsonObject findings = parseFindingsObject(findingsString);
        if (findings != null) {
          for (String pmid : findings.keySet()) {
            if (!citationList.contains(pmid)) {
//              throw new RuntimeException("PMID ("+pmid+") used in Findings not in PMID field, row " + readableRow);
              sf_logger.warn("PMID ("+pmid+") used in Findings not in PMID field, row " + readableRow);
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
              findings,
              comments
          );
        }
        catch (RuntimeException ex) {
          throw new RuntimeException("Error on row " + readableRow, ex);
        }
      }
      
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        row = workbook.getRow(rowIdx);
        if (!row.hasNoText(0) && !row.getNullableText(0).toLowerCase().startsWith("note")) {
          dbHarness.insertNote(row.getNullableText(0));
        }
      }
      
      workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        row = workbook.getRow(i);
        if (row.hasNoText(0) ^ row.hasNoText(1)) {
          throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
        } else if (row.hasNoText(0) ) continue;

        java.util.Date date = row.getDate(0);
        String note = row.getText(1);
        dbHarness.insertChange(date, note);
      }
    }

    if (noFunctionAlleles.size() > 0) {
      sf_logger.warn("No clinical function assigned to {}", String.join("; ", noFunctionAlleles));
    }
  }

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
  static JsonObject parseFindingsObject(String findings) {
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
    return findingsObject;
  }


  /**
   * Private class for handling DB interactions
   */
  static class DbHarness implements AutoCloseable {
    private final Connection conn;
    private final Map<String, Long> alleleNameMap = new HashMap<>();
    private final PreparedStatement insertAlleleStmt;
    private final PreparedStatement insertNoteStmt;
    private final PreparedStatement insertChangeStmt;
    private final String gene;
    private final LookupMethod geneLookupMethod;
    private int noteIdx = 0;

    DbHarness(String gene) throws SQLException {
      this.gene = gene;
      this.conn = ConnectionFactory.newConnection();

      try (PreparedStatement pstmt = this.conn.prepareStatement("select name, id from allele_definition where geneSymbol=?")) {
        pstmt.setString(1, gene);
        try (ResultSet rs = pstmt.executeQuery()) {
          while (rs.next()) {
            this.alleleNameMap.put(rs.getString(1), rs.getLong(2));
          }
        }
      }

      if (this.alleleNameMap.size() == 0) {
        throw new RuntimeException("No alleles found for gene: " + gene);
      }

      try (PreparedStatement pstmt = this.conn.prepareStatement("select lookupmethod from gene where symbol=?")) {
        pstmt.setString(1, gene);
        try (ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            this.geneLookupMethod = LookupMethod.valueOf(rs.getString(1));
          } else {
            sf_logger.warn("Gene lookup method not found for " + gene);
            this.geneLookupMethod = null;
          }
        }
      }

      insertAlleleStmt = this.conn.prepareStatement("insert into allele(geneSymbol, name, definitionId, functionalStatus, activityvalue, clinicalfunctionalstatus, clinicalFunctionalSubstrate, citations, strength, findings, functioncomments) values (?, ?, ?, initcap(?), ?, initcap(?), ?, ?, ?, ?::jsonb, ?)");
      insertNoteStmt = this.conn.prepareStatement("insert into file_note(entityId, note, type, ordinal) values (?, ?, ?, ?)");
      insertChangeStmt = this.conn.prepareStatement("insert into change_log(entityId, note, type, date) values (?, ?, ?, ?)");
    }

    private Long lookupAlleleDefinitionId(String alleleName) {
      String alleleDefinitionName = parseAlleleDefinitionName(alleleName);
      if (!this.alleleNameMap.containsKey(alleleDefinitionName)) {
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
        JsonObject findings,
        String comments
    ) throws SQLException {
      Long alleleDefinitionId = lookupAlleleDefinitionId(allele);

      if (StringUtils.isBlank(activityValue) && geneLookupMethod == LookupMethod.ACTIVITY_SCORE) {
        sf_logger.warn("{} is missing an activity score", allele);
      }

      this.insertAlleleStmt.clearParameters();
      this.insertAlleleStmt.setString(1, this.gene);
      this.insertAlleleStmt.setString(2, allele);
      this.insertAlleleStmt.setLong(3, alleleDefinitionId);
      this.insertAlleleStmt.setString(4, normalizeFunction(alleleFunction));
      setNullableText(this.insertAlleleStmt, 5, activityValue);
      setNullableText(this.insertAlleleStmt, 6, clinicalFunction);
      setNullableText(this.insertAlleleStmt, 7, substrate);
      this.insertAlleleStmt.setArray(8, conn.createArrayOf("TEXT", pmids));
      setNullableText(this.insertAlleleStmt, 9, strength);
      setNullableText(this.insertAlleleStmt, 10, findings != null ? findings.toString() : null);
      setNullableText(this.insertAlleleStmt, 11, comments);
      int inserted = this.insertAlleleStmt.executeUpdate();
      if (inserted == 0) {
        throw new RuntimeException("No allele inserted");
      }
    }

    void insertNote(String note) throws SQLException {
      this.insertNoteStmt.clearParameters();
      this.insertNoteStmt.setString(1, gene);
      this.insertNoteStmt.setString(2, note);
      this.insertNoteStmt.setString(3, NoteType.FUNCTION_REFERENCE.name());
      this.insertNoteStmt.setInt(4, this.noteIdx);
      this.insertNoteStmt.executeUpdate();
      this.noteIdx += 1;
    }
    
    /**
     * Insert a change event into the history table.
     * @param date the date of the change, required
     * @param note the text note to explain the change
     * @throws SQLException can occur from bad database transaction
     */
    void insertChange(java.util.Date date, String note) throws SQLException {
      Preconditions.checkNotNull(date, "History line has a blank date");

      this.insertChangeStmt.clearParameters();
      this.insertChangeStmt.setString(1, gene);
      if (StringUtils.isNotBlank(note)) {
        this.insertChangeStmt.setString(2, note);
      } else {
        this.insertChangeStmt.setString(2, "n/a");
      }
      this.insertChangeStmt.setString(3, NoteType.FUNCTION_REFERENCE.name());
      this.insertChangeStmt.setDate(4, new Date(date.getTime()));
      this.insertChangeStmt.executeUpdate();
    }
    
    private void setNullableText(PreparedStatement stmt, int idx, String value) throws SQLException {
      if (StringUtils.isNotBlank(value)) {
        if (value.equals("N/A")) {
          stmt.setString(idx, "n/a");
        } else {
          stmt.setString(idx, value);
        }
      } else {
        stmt.setNull(idx, Types.VARCHAR);
      }
    }

    @Override
    public void close() throws SQLException {
      if (this.insertAlleleStmt != null) {
        this.insertAlleleStmt.close();
      }
      if (this.insertNoteStmt != null) {
        this.insertNoteStmt.close();
      }
      if (this.insertChangeStmt != null) {
        this.insertChangeStmt.close();
      }
      if (this.conn != null) {
        this.conn.close();
      }
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
