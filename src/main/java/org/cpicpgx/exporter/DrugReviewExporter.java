package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Exports a report of non-guideline Drugs in the database with their related IDs
 *
 * @author Ryan Whaley
 */
public class DrugReviewExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    DrugReviewExporter exporter = new DrugReviewExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception e) {
      sf_logger.error("Error exporting drug review", e);
    }
  }

  FileType getFileType() {
    return FileType.DRUG_REVIEW;
  }

  EntityType getEntityCategory() {
    return EntityType.DRUG;
  }
  
  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement(
             "select drugid, name, pharmgkbid, rxnormid, drugbankid, atcid " +
                 "from drug d where d.drugid not in " +
                 "(select drugid from pair p where p.guidelineid is not null) order by lower(name)");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      DrugReviewWorkbook workbook = new DrugReviewWorkbook();
      
      while (grs.next()) {
        String id = grs.getString(1);
        String name = grs.getString(2);
        String pgkb = grs.getString(3);
        String rxnorm = grs.getString(4);
        String drugbank = grs.getString(5);
        String[] atcCodes = grs.getArray(6) == null ? new String[0] : (String[])grs.getArray(6).getArray();

        workbook.writeDrug(name, id, pgkb, rxnorm, drugbank, atcCodes);
      }
      writeWorkbook(workbook);
      handleFileUpload();
      addFileExportHistory(workbook.getFilename(), new String[]{});
    }
  }
}
