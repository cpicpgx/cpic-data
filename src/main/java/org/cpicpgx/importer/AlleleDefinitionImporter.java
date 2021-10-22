package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.exporter.AbstractWorkbook;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class crawls the given directory for <code>.xlsx</code> files and runs on each gene file in succession.
 *
 * @author Ryan Whaley
 */
public class AlleleDefinitionImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  //language=PostgreSQL
  private static final String[] sf_deleteStatements = new String[]{
      "delete from change_log where type='" + FileType.ALLELE_DEFINITION.name() + "'",
      "delete from file_note where type='" + FileType.ALLELE_DEFINITION.name() + "'",
      "delete from allele_location_value where locationId is not null",
  };
  private static final int sf_variantColStart = 1;
  private static final Pattern sf_seqIdPattern = Pattern.compile("N\\D_\\d+\\.\\d+");
  private static final Pattern sf_seqPositionPattern = Pattern.compile("[gm]\\.((\\d+)(_(\\d+))?)");
  private static final Pattern sf_wobbleCodePattern = Pattern.compile("[BD-FH-SU-Z]");
  private static final Pattern sf_rsidPattern = Pattern.compile("^rs\\d+$");
  private static final int sf_alleleRowStart = 7;

  private String m_gene;
  private int m_variantColEnd;
  private String m_proteinSeqId = "";
  private String m_chromoSeqId = "";
  private String m_geneSeqId = "";
  private String m_mrnaSeqId = "";
  private String[] m_legacyNames;
  private String[] m_proteinEffects;
  private String[] m_chromoPositions;
  private Integer[] m_chromoStartPositions;
  private String[] m_genoPositions;
  private String[] m_dbSnpIds;
  private Map<String,Map<Integer,String>> m_alleles;
  private Map<String,String> m_svTextMap;
  private int m_svColIdx = -1;

  public static void main(String[] args) {
    rebuild(new AlleleDefinitionImporter(), args);
  }

  public AlleleDefinitionImporter() { }

  @Override
  public FileType getFileType() {
    return FileType.ALLELE_DEFINITION;
  }
  
  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return Constants.EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    readGene(workbook);
    readLegacyRow(workbook);
    readProteinRow(workbook);
    readChromoRow(workbook);
    readGenoRow(workbook);
    readDbSnpRow(workbook);
    readAlleles(workbook);
    writeToDB();

    writeNotes(m_gene, workbook.getNotes());
    writeHistory(workbook);
  }

  private void readGene(WorkbookWrapper workbook) {
    RowWrapper row = workbook.getRow(0);
    m_gene = row.getText(0)
        .replaceAll("(GENE|Gene):\\s*", "");
  }

  private void readLegacyRow(WorkbookWrapper workbook) {
    RowWrapper row = workbook.getRow(1);
    m_legacyNames = new String[row.getLastCellNum()];
    m_svColIdx = -1;

    findSeqId(row.getNullableText(0));

    for (int i=sf_variantColStart; i < row.getLastCellNum(); i++) {
      String cellContents = row.getNullableText(i);
      if (cellContents != null && cellContents.equalsIgnoreCase(Constants.STRUCTURAL_VARIATION)) {
        m_svColIdx = i;
      } else {
        m_legacyNames[i] = cellContents;
        m_variantColEnd = i;
      }
    }
  }

  private void readProteinRow(WorkbookWrapper workbook) {
    RowWrapper row = workbook.getRow(2);
    m_proteinEffects = new String[m_variantColEnd + 1];

    findSeqId(row.getNullableText(0));

    for (int i=sf_variantColStart; i <= m_variantColEnd; i++) {
      m_proteinEffects[i] = row.getNullableText(i);
    }
  }

  private void readChromoRow(WorkbookWrapper workbook) {
    RowWrapper row = workbook.getRow(3);
    m_chromoPositions = new String[row.getLastCellNum()];
    m_chromoStartPositions = new Integer[row.getLastCellNum()];

    String description = row.getNullableText(0);
    findSeqId(description);

    for (int i=sf_variantColStart; i <= m_variantColEnd; i++) {
      m_chromoPositions[i] = row.getNullableText(i);
      m_chromoStartPositions[i] = checkPosition(m_chromoPositions[i]);
    }
  }

  private void readGenoRow(WorkbookWrapper workbook) {
    RowWrapper row = workbook.getRow(4);
    m_genoPositions = new String[m_variantColEnd + 1];

    findSeqId(row.getNullableText(0));

    for (int i=sf_variantColStart; i <=m_variantColEnd; i++) {
      m_genoPositions[i] = row.getNullableText(i);
    }
  }

  private void readDbSnpRow(WorkbookWrapper workbook) {
    RowWrapper row = workbook.getRow(5);
    m_dbSnpIds = new String[row.getLastCellNum()];

    for (int i=sf_variantColStart; i <= m_variantColEnd; i++) {
      String rsidField = row.getNullableText(i);
      if (rsidField == null) {
        continue;
      }

      String[] rsids = rsidField.split(";\\s*");
      if (Stream.of(rsids).allMatch((r) -> sf_rsidPattern.matcher(r).matches())) {
        m_dbSnpIds[i] = rsidField;
      } else {
        sf_logger.warn("Invalid RSID found in {}, skipping", row.getAddress(i));
      }
    }
  }

  private void findSeqId(String cellContent) {
    if (StringUtils.isBlank(cellContent)) return;

    Matcher m = sf_seqIdPattern.matcher(cellContent);
    if (m.find()) {
      String seqId = m.group();
      if (seqId.startsWith("NG_")) {
        m_geneSeqId = seqId;
      } else if (seqId.startsWith("NM_")) {
        m_mrnaSeqId = seqId;
      } else if (seqId.startsWith("NC_")) {
        m_chromoSeqId = seqId;
      } else if (seqId.startsWith("NP_")) {
        m_proteinSeqId = seqId;
      }
    }
  }

  /**
   * Check to make sure the position found in this cell matches the expected format and doesn't include a wobble
   * @param cellContent chromosomal cell content
   * @return the Integer start position for the chromosomal value
   */
  Integer checkPosition(String cellContent) {
    if (StringUtils.isBlank(cellContent)) return null;

    Matcher wm = sf_wobbleCodePattern.matcher(cellContent);
    if (wm.find()) {
      String code = wm.group(0);
      throw new RuntimeException("Found a wobble code in chromosome posiiton, [" + code + "]");
    }

    Matcher m = sf_seqPositionPattern.matcher(cellContent);
    if (m.find()) {
      return Integer.valueOf(m.group(2));
    } else {
      throw new RuntimeException("No position found for [" + cellContent + "]");
    }
  }

  private void readAlleles(WorkbookWrapper workbook) {
    m_alleles = new LinkedHashMap<>();
    m_svTextMap = new HashMap<>();
    for (int i=sf_alleleRowStart; i <= workbook.currentSheet.getLastRowNum(); i++) {
      try {
        RowWrapper row = workbook.getRow(i);
        if (row == null) {
          continue;
        }

        String alleleName = row.getNullableText(0);
        if (alleleName == null) {
          continue;
        }

        if (alleleName.toLowerCase().startsWith("notes")) {
          throw new RuntimeException("Notes exist in the allele definition sheet, move to a separate tab");
        }

        if (m_svColIdx >=0) {
          String svText = row.getNullableText(m_svColIdx);
          if (svText != null) {
            m_svTextMap.put(alleleName, svText);
          }
        }

        Map<Integer, String> definition = new LinkedHashMap<>();
        for (int j = sf_variantColStart; j <= m_variantColEnd; j++) {
          if (row.getNullableText(j) != null) {
            definition.put(j, row.getText(j));
          }
        }

        m_alleles.put(alleleName, definition);
      } catch (Exception e) {
        sf_logger.error("Error parsing row {}", i+1);
        throw e;
      }
    }
  }

  void writeHistory(WorkbookWrapper workbook) throws SQLException {
    workbook.currentSheetIs(AbstractWorkbook.HISTORY_SHEET_NAME);

    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement insertStmt = conn.prepareStatement("insert into change_log (entityId, type, date, note) values (?, ?, ?, ?)");
      insertStmt.setString(1, m_gene);
      insertStmt.setString(2, FileType.ALLELE_DEFINITION.name());

      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        if (row.hasNoText(0) ^ row.hasNoText(1)) {
          throw new RuntimeException("Change log row " + (i + 1) + ": row must have both date and text");
        }
        else if (row.hasNoText(0)) continue;

        Date date = row.getDate(0);
        String note = row.getNullableText(1);

        if (note.equalsIgnoreCase(AbstractWorkbook.LOG_FILE_CREATED)) continue;

        insertStmt.setDate(3, new java.sql.Date(date.getTime()));
        if (StringUtils.isNotBlank(note)) {
          insertStmt.setString(4, note);
        } else {
          insertStmt.setString(4, Constants.NA);
        }

        insertStmt.executeUpdate();
      }
    }
  }

  void writeToDB() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement selectExistingAlleles = conn.prepareStatement("select name from allele_definition where genesymbol=?");
      selectExistingAlleles.setString(1, m_gene);
      Set<String> alleleNamesNotUpdated = new HashSet<>();
      try (ResultSet existingRs = selectExistingAlleles.executeQuery()) {
        while (existingRs.next()) {
          alleleNamesNotUpdated.add(existingRs.getString(1));
        }
      }

      PreparedStatement joinTableInsert = conn.prepareStatement(
          "insert into allele_location_value(alleledefinitionid, locationid, variantallele) values (?,?,?)");
      PreparedStatement joinTableDelete = conn.prepareStatement(
          "delete from allele_location_value where alleledefinitionid=?");

      PreparedStatement geneUpdate = conn.prepareStatement(
          "update gene set genesequenceid=?,proteinsequenceid=?,chromosequenceid=?,mrnaSequenceId=? where symbol=?");
      geneUpdate.setString(1, m_geneSeqId);
      geneUpdate.setString(2, m_proteinSeqId);
      geneUpdate.setString(3, m_chromoSeqId);
      geneUpdate.setString(4, m_mrnaSeqId);
      geneUpdate.setString(5, m_gene);
      geneUpdate.executeUpdate();

      PreparedStatement seqLocInsert = conn.prepareStatement(
          "insert into sequence_location(name, chromosomelocation, genelocation, proteinlocation, dbsnpid, geneSymbol, position) " +
              "values (?,?,?,?,?,?, ?) " +
              "on conflict (chromosomelocation) do update set name=excluded.name, chromosomelocation=excluded.chromosomelocation, genelocation=excluded.genelocation, proteinlocation=excluded.proteinlocation, dbsnpid=excluded.dbsnpid " +
              "returning (id)");
      Integer[] locIdAssignements = new Integer[m_chromoPositions.length];
      int newLocations = 0;
      for (int i=0; i < m_chromoPositions.length; i++) {

        // here we want to guard against over-running the location columns
        // we can't rely on either the legacy row or the chromo row singly since there are sheets that have missing
        // values in both so we need to check for either
        if (m_chromoPositions[i] == null && m_legacyNames[i] == null) {
          continue;
        }

        seqLocInsert.setString(1, m_legacyNames[i]);
        seqLocInsert.setString(2, m_chromoPositions[i]);
        seqLocInsert.setString(3, m_genoPositions[i]);
        seqLocInsert.setString(4, m_proteinEffects[i]);
        if (i < m_dbSnpIds.length && m_dbSnpIds[i] != null) {
          seqLocInsert.setString(5, m_dbSnpIds[i]);
        } else {
          seqLocInsert.setNull(5, Types.VARCHAR);
        }
        seqLocInsert.setString(6, m_gene);
        seqLocInsert.setInt(7, m_chromoStartPositions[i]);
        ResultSet rs = seqLocInsert.executeQuery();
        rs.next();
        int locId = rs.getInt(1);
        locIdAssignements[i] = locId;
        newLocations += 1;
      }
      sf_logger.debug("created {} new locations", newLocations);

      PreparedStatement alleleDefInsert = conn.prepareStatement(
          "insert into allele_definition(geneSymbol, name, reference, structuralvariation) values (?,?,?,?) " +
              "on conflict (genesymbol,name) do update set reference=excluded.reference, structuralvariation=excluded.structuralvariation, pharmvarid=excluded.pharmvarid " +
              "returning (id)");
      PreparedStatement alleleInsert = conn.prepareStatement(
          "insert into allele(genesymbol, name, definitionId) values (?,?,?) on conflict do nothing");
      boolean isReference = true;
      for (String alleleName : m_alleles.keySet()) {
        alleleDefInsert.setString(1, m_gene);
        alleleDefInsert.setString(2, alleleName);
        alleleDefInsert.setBoolean(3, isReference);
        alleleDefInsert.setBoolean(4, m_svTextMap.containsKey(alleleName));
        ResultSet rs = alleleDefInsert.executeQuery();
        rs.next();
        int alleleId = rs.getInt(1);

        alleleInsert.clearParameters();
        alleleInsert.setString(1, m_gene);
        alleleInsert.setString(2, alleleName);
        alleleInsert.setInt(3, alleleId);
        alleleInsert.executeUpdate();

        joinTableDelete.setInt(1, alleleId);
        joinTableDelete.executeUpdate();
        Map<Integer,String> allelePosMap = m_alleles.get(alleleName);
        for (Integer locIdx : allelePosMap.keySet()) {
          joinTableInsert.setInt(1, alleleId);
          joinTableInsert.setInt(2, locIdAssignements[locIdx]);
          joinTableInsert.setString(3, allelePosMap.get(locIdx));
          joinTableInsert.executeUpdate();
        }
        isReference = false;
        alleleNamesNotUpdated.remove(alleleName);
      }
      sf_logger.debug("processed {} alleles", m_alleles.keySet().size());

      if (alleleNamesNotUpdated.size() > 0) {
        PreparedStatement deleteAllele = conn.prepareStatement("delete from allele where definitionid=(select id from allele_definition where genesymbol=? and name=?)");
        PreparedStatement deleteAlleleDef = conn.prepareStatement("delete from allele_definition where genesymbol=? and name=?");
        PreparedStatement deleteLocValue = conn.prepareStatement("delete from allele_location_value where alleledefinitionid=(select id from allele_definition where genesymbol=? and name=?)");

        for (String alleleName : alleleNamesNotUpdated) {
          deleteAllele.setString(1, m_gene);
          deleteAllele.setString(2, alleleName);
          deleteAllele.executeUpdate();

          deleteLocValue.setString(1, m_gene);
          deleteLocValue.setString(2, alleleName);
          deleteLocValue.executeUpdate();

          deleteAlleleDef.setString(1, m_gene);
          deleteAlleleDef.setString(2, alleleName);
          deleteAlleleDef.executeUpdate();
          sf_logger.warn("removed allele: {} {}", m_gene, alleleName);
        }
      }

      try (PreparedStatement deleteUnusedLocs = conn.prepareStatement("delete from sequence_location where id not in (select locationid from allele_location_value) and genesymbol=?")) {
        deleteUnusedLocs.setString(1, m_gene);
        int deletedLocations = deleteUnusedLocs.executeUpdate();
        if (deletedLocations > 0) {
          sf_logger.warn("removed {} unused sequence locations", deletedLocations);
        }
      }
    }
  }
}
