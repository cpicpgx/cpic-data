package org.cpicpgx.importer;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.function.Consumer;

/**
 * Parses gene mapping files for NCBI, HGNC, and Ensembl data. Those IDs are then set in the gene table
 *
 * @author Ryan Whaley
 */
public class GeneReferenceImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      GeneReferenceImporter importer = new GeneReferenceImporter();
      importer.parseArgs(args);
      importer.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse gene data", e);
    }
  }
  
  private GeneReferenceImporter() {}
  
  public GeneReferenceImporter(Path directory) {
    this.setDirectory(directory);
  }
  
  public void execute() {
    streamFiles()
        .filter(filterFileFunction(EXCEL_EXTENSION))
        .forEach(processFile);
  }
  
  private Consumer<File> processFile = (File file) -> {
    sf_logger.info("Reading {}", file);
    
    try (InputStream in = Files.newInputStream(file.toPath())) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);
      processWorkbook(workbook);
    } catch (Exception ex) {
      throw new RuntimeException("Error processing file " + file, ex);
    }
  };
  
  private void processWorkbook(WorkbookWrapper workbook) throws Exception {
    try (Connection conn = ConnectionFactory.newConnection()) {
      PreparedStatement hgncstmt = conn.prepareStatement("update gene set hgncid=? where symbol=?");
      PreparedStatement ncbistmt = conn.prepareStatement("update gene set ncbiid=? where symbol=?");
      PreparedStatement ensstmt  = conn.prepareStatement("update gene set ensemblid=? where symbol=?");

      for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        String idType = row.getNullableText(2);
        String idValue = row.getNullableText(3, true);
        String symbolValue = row.getNullableText(0);
      
        PreparedStatement updateStmt = null;
        if (idValue != null) {
          switch (idType) {
            case "HGNC ID":
              updateStmt = hgncstmt;
              break;
            case "Gene ID":
              updateStmt = ncbistmt;
              break;
            case "Ensembl ID":
              updateStmt = ensstmt;
              break;
            default:
              // fall out
          }
        }
        
        if (updateStmt != null) {
          updateStmt.setString(1, idValue);
          updateStmt.setString(2, symbolValue);
          updateStmt.executeUpdate();
        }
      }
    }
  }
}
