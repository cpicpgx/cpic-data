package org.cpicpgx.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parses the PharmVar ID list TSV file. This updates the existing alleles table so make sure allele data is loaded 
 * prior to running this. The expected columns of the input file are:
 * <ol>
 *   <li>Gene</li>
 *   <li>Gene+Allele</li>
 *   <li>ID (for non-core alleles)</li>
 *   <li>ID (for core alleles)</li>
 * </ol>
 *
 * @author Ryan Whaley
 */
public class PharmVarImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String PROCESS_FILE_EXTENSION = ".tsv";
  private static final String DEFAULT_DIRECTORY = "pharmvar";

  public static void main(String[] args) {
    rebuild(new PharmVarImporter(), args);
  }
  
  @Override
  String getFileExtensionToProcess() {
    return PROCESS_FILE_EXTENSION;
  }

  @Override
  FileType getFileType() {
    return FileType.PHARMVAR;
  }

  @Override
  String[] getDeleteStatements() {
    return new String[0];
  }

  @Override
  String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }
  
  @Override
  Consumer<File> getFileProcessor() {
    return f -> {
      sf_logger.info("Processing file {}", f);
      try (
          Reader reader = new FileReader(f);
          DbHarness dbHarness = new DbHarness()
      ) {
        int line = 0;
        for (CSVRecord record : CSVFormat.TDF.parse(reader)) {
          if (line != 0) {
            String gene = record.get(0);
            String geneAllele = record.get(1);
            String allele = geneAllele.replaceAll(gene, "");
            String id;
            if (record.size() == 3) {
              id = record.get(2);
            } else {
              id = record.get(3);
            }
            
            dbHarness.updateId(gene, allele, id);
          }
          line += 1;
        }
        addImportHistory(f.getName());
      } catch (FileNotFoundException e) {
        throw new RuntimeException("No file " + f, e);
      } catch (IOException e) {
        throw new RuntimeException("Error reading " + f, e);
      } catch (SQLException e) {
        throw new RuntimeException("Error with database", e);
      }
    };
  }

  private static class DbHarness implements AutoCloseable {
    private List<AutoCloseable> closables = new ArrayList<>();
    private PreparedStatement updateStmt;
    
    DbHarness() throws SQLException {
      Connection conn = ConnectionFactory.newConnection();
      closables.add(conn);

      updateStmt = conn.prepareStatement("update allele_definition set pharmvarId=? where geneSymbol=? and name=?");
      closables.add(updateStmt);
    }
    
    private void updateId(String gene, String allele, String id) throws SQLException {
      if (allele.matches(".+\\.\\d{3}")) return;
      
      updateStmt.setString(1, id);
      updateStmt.setString(2, gene);
      updateStmt.setString(3, allele);
      int n = updateStmt.executeUpdate();
      if (n == 0) {
        sf_logger.info("PharmVar allele not in CPIC: {} {}", gene, allele);
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

