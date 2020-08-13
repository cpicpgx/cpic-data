package org.cpicpgx.importer;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.exception.NotFoundException;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses gene CDS language files
 *
 * @author Ryan Whaley
 */
public class GeneCdsImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern GENE_PATTERN = Pattern.compile("([\\w-]+)\\s+[Pp]henotype");
  private static final String[] sf_deleteStatements = new String[]{
      "delete from file_note where type='" + NoteType.CDS.name() + "'",
      "delete from change_log where type='" + NoteType.CDS.name() + "'"
  };
  private static final String DEFAULT_DIRECTORY = "gene_cds";
  private static final int COL_PHENOTYPE = 0;
  private static final int COL_ACTIVITY = 1;
  private static final int COL_EHR_PRIORITY = 2;
  private static final int COL_CONSULTATION = 3;

  public static void main(String[] args) {
    rebuild(new GeneCdsImporter(), args);
  }
  
  public GeneCdsImporter() { }
  
  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.GENE_CDS;
  }

  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }
  
  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    workbook.switchToSheet(0);
    int rowIdx = 0;

    sf_logger.debug("Reading CDS sheet: {}", workbook.currentSheet.getSheetName());

    RowWrapper headerRow = workbook.getRow(rowIdx);
    rowIdx += 1;
    String geneText = headerRow.getNullableText(0);
    if (geneText == null) {
      throw new NotFoundException("Couldn't find gene");
    }
    Matcher m = GENE_PATTERN.matcher(geneText);
    if (!m.find()) {
      sf_logger.warn("No gene found for workbook {}, skipping", workbook.getFileName());
      return;
    }
    String geneSymbol = m.group(1);
    sf_logger.debug("loading gene {}", geneSymbol);

    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        RowWrapper dataRow = workbook.getRow(rowIdx);
        if (dataRow.hasNoText(COL_PHENOTYPE)) continue;

        String activity = normalizeScore(dataRow.getNullableText(COL_ACTIVITY));
        
        String pheno = dataRow.getNullableText(COL_PHENOTYPE);
        if (pheno.toLowerCase().startsWith("notes")) {
          rowIdx++;
          break;
        }

        String priority = dataRow.getText(COL_EHR_PRIORITY);
        String consultation = dataRow.getText(COL_CONSULTATION);

        dbHarness.insert(pheno, activity, priority, consultation);
      }
      
      // pick up any note rows after "Notes:" header
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        RowWrapper row = workbook.getRow(rowIdx);
        if (row.hasNoText(0)) continue;
        dbHarness.insertNote(row.getNullableText(0));
      }
    }
  }

  static class DbHarness implements AutoCloseable {
    private final Connection conn;
    private final String gene;
    private final PreparedStatement insertStmt;
    private final PreparedStatement insertNote;
    private int noteOrdinal = 0;

    DbHarness(String gene) throws SQLException {
      this.gene = gene;
      this.conn = ConnectionFactory.newConnection();

      insertStmt = this.conn.prepareStatement(
          "insert into gene_phenotype(geneSymbol, phenotype, ehrPriority, consultationText, activityScore) values (?, ?, ?, ?, ?) ON CONFLICT (genesymbol, phenotype, activityScore) DO UPDATE set ehrpriority=excluded.ehrpriority, consultationtext=excluded.consultationtext"
      );
      insertNote = this.conn.prepareStatement("insert into file_note(entityId, note, type, ordinal) values (?, ?, ?, ?)");
    }

    void insert(String phenotype, String activity, String ehr, String consultation) throws SQLException {
      String normalizedPhenotype = phenotype.replaceAll(this.gene + "\\s+", "");

      insertStmt.clearParameters();
      insertStmt.setString(1, gene);
      insertStmt.setString(2, normalizedPhenotype);
      insertStmt.setString(3, ehr);
      insertStmt.setString(4, consultation);
      insertStmt.setString(5, activity);
      insertStmt.executeUpdate();
    }
    
    void insertNote(String note) throws SQLException {
      insertNote.clearParameters();
      insertNote.setString(1, gene);
      insertNote.setString(2, note);
      insertNote.setString(3, NoteType.CDS.name());
      insertNote.setInt(4, noteOrdinal);
      insertNote.executeUpdate();
      noteOrdinal += 1;
    }
    
    @Override
    public void close() throws Exception {
      if (this.insertStmt != null) {
        this.insertStmt.close();
      }
      if (this.insertNote != null) {
        this.insertNote.close();
      }
      if (this.conn != null) {
        this.conn.close();
      }
    }
  }
}
