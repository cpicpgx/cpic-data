package org.cpicpgx.importer;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.NoteType;
import org.cpicpgx.exception.NotFoundException;
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
  private static final Pattern GENE_PATTERN = Pattern.compile("Gene:\\s*(\\w+)");
  private static final String[] sf_deleteStatements = new String[]{
      "delete from gene_note where type='" + NoteType.CDS.name() + "'",
      "delete from gene_phenotype"
  };
  private static final String DEFAULT_DIRECTORY = "gene_cds";
  private static final int COL_PHENO = 0;
  private static final int COL_EHR_PRIORITY = 1;
  private static final int COL_CONSULTATION = 2;
  private static final int COL_NOTES = 3;

  public static void main(String[] args) {
    rebuild(new GeneCdsImporter(), args);
  }
  
  public GeneCdsImporter() { }
  
  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

  @Override
  public FileType getFileType() {
    return FileType.DIPLOTYPE_PHENOTYPE;
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
    sf_logger.info("Reading CDS sheet: {}", workbook.currentSheet.getSheetName());

    RowWrapper geneRow = workbook.getRow(0);
    String geneText = geneRow.getNullableText(0);
    if (geneText == null) {
      throw new NotFoundException("Couldn't find gene");
    }

    Matcher m = GENE_PATTERN.matcher(geneText);
    if (!m.find()) {
      throw new NotFoundException("Couldn't find gene");
    }
    String geneSymbol = m.group(1);
    sf_logger.debug("loading gene {}", geneSymbol);

    try (DbHarness dbHarness = new DbHarness(geneSymbol)) {
      // skip 2 header rows
      int rowIdx = 2;
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        RowWrapper row = workbook.getRow(rowIdx);
        if (row.hasNoText(COL_PHENO)) continue;
        
        String pheno = row.getNullableText(COL_PHENO);
        if (pheno.toLowerCase().startsWith("notes:")) {
          rowIdx++;
          break;
        }

        String priority = row.getNullableText(COL_EHR_PRIORITY);
        String consultation = row.getNullableText(COL_CONSULTATION);
        String notes = row.getNullableText(COL_NOTES);
        
        dbHarness.insert(pheno, priority, consultation, notes);
      }
      
      // pick up any note rows after "Notes:" header
      for (; rowIdx <= workbook.currentSheet.getLastRowNum(); rowIdx++) {
        RowWrapper row = workbook.getRow(rowIdx);
        if (row.hasNoText(0)) continue;
        dbHarness.insertNote(row.getNullableText(0));
      }
    }
    addImportHistory(workbook.getFileName());
  }

  static class DbHarness implements AutoCloseable {
    private Connection conn;
    private String gene;
    private PreparedStatement insertStmt;
    private PreparedStatement insertNote;
    private int noteOrdinal = 0;

    DbHarness(String gene) throws SQLException {
      this.gene = gene;
      this.conn = ConnectionFactory.newConnection();

      insertStmt = this.conn.prepareStatement(
          "insert into gene_phenotype(geneSymbol, phenotype, ehrPriority, consultationText, notes) values (?, ?, ?, ?, ?)"
      );
      insertNote = this.conn.prepareStatement("insert into gene_note(geneSymbol, note, type, ordinal) values (?, ?, ?, ?)");
    }

    void insert(String phenotype, String ehr, String consultation, String notes) throws SQLException {
      insertStmt.clearParameters();
      insertStmt.setString(1, gene);
      insertStmt.setString(2, phenotype);
      insertStmt.setString(3, ehr);
      insertStmt.setString(4, consultation);
      insertStmt.setString(5, notes);
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
