package org.cpicpgx.importer;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Import the list of all CPIC guidelines and their PMIDs
 *
 * @author Ryan Whaley
 */
public class GuidelineImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String[] sf_deleteStatements = new String[]{};
  private static final String DEFAULT_DIRECTORY = "guidelines";

  public static void main(String[] args) {
    try {
      GuidelineImporter importer = new GuidelineImporter();
      importer.parseArgs(args);
      importer.execute();
    } catch (Exception ex) {
      sf_logger.error("Error importing guidelines", ex);
    }
  }
  
  private GuidelineImporter() {}
  
  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
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
  void processWorkbook(WorkbookWrapper workbook) throws SQLException {
    
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement insertStmt = conn.prepareStatement("insert into guideline(name, url) values (?, ?) returning (id)");
        PreparedStatement updatePub = conn.prepareStatement("update publication set guidelineid=? where pmid=?");
        PreparedStatement updatePair = conn.prepareStatement("update pair set guidelineid=? where ? = any (citations)")
    ) {

      int guidelineCount = 0;
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        if (row.hasNoText(0)) {
          continue;
        }
        String name = row.getNullableText(0);
        String url = row.getNullableText(1);
        String pmids = row.getNullableText(2);

        insertStmt.setString(1, name);
        insertStmt.setString(2, url);
        
        try (ResultSet rs = insertStmt.executeQuery()) {
          if (rs.next()) {
            long guidelineId = rs.getLong(1);
            Arrays.stream(pmids.split(",")).forEach((pmid) -> {
              try {
                updatePub.setLong(1, guidelineId);
                updatePub.setString(2, pmid);
                updatePub.executeUpdate();

                updatePair.setLong(1, guidelineId);
                updatePair.setString(2, pmid);
                updatePair.executeUpdate();
              } catch (SQLException ex) {
                throw new RuntimeException("Error updating publication", ex);
              }
            });
          }
        }
        guidelineCount += 1;
      }
      sf_logger.info("loaded {} guidelines", guidelineCount);
    }
  }
}
