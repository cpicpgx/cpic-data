package org.cpicpgx.importer;

import com.google.gson.Gson;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * This importer will get data from JSON files supplied by PharmGKB to update genes-drugs pairs data. This will only 
 * update existing pairs and won't add or remove pairs.
 *
 * @author Ryan Whaley
 */
public class PairDiffImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String PROCESS_FILE_EXTENSION = ".json";
  private static final String DEFAULT_DIRECTORY = "pairsDiffs";

  public static void main(String[] args) {
    rebuild(new PairDiffImporter(), args);
  }
  
  @Override
  String getFileExtensionToProcess() {
    return PROCESS_FILE_EXTENSION;
  }

  @Override
  FileType getFileType() {
    return FileType.PAIR_DIFF;
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
          FileReader fileReader = new FileReader(f);
          DbHarness dbHarness = new DbHarness()
      ) {
        Gson gson = new Gson();
        PairDiff[] diffs = gson.fromJson(fileReader, PairDiff[].class);
        sf_logger.info("Found {} diffs", diffs.length);
        
        for (PairDiff pairDiff : diffs) {
          if (pairDiff.pairid != null) {
            Arrays.stream(pairDiff.messages).forEach(sf_logger::info);
            int changes = dbHarness.update(pairDiff.pairid, pairDiff.pgkbcalevel, pairDiff.pgxtesting);
            sf_logger.info("{} changes applied to {}", changes, pairDiff.name);
          }
        }
        addImportHistory(f.getName());
      } catch (Exception e) {
        throw new RuntimeException("Error reading diffs", e);
      }
    };
  }
  
  private static class PairDiff {
    String[] messages;
    String name;
    Long pairid;
    String pgkbcalevel;
    String pgxtesting;
  }

  private static class DbHarness implements AutoCloseable {
    private List<AutoCloseable> closables = new ArrayList<>();
    private PreparedStatement updateLevelStmt;
    private PreparedStatement updateTestingStmt;

    DbHarness() throws SQLException {
      Connection conn = ConnectionFactory.newConnection();
      closables.add(conn);

      updateLevelStmt = conn.prepareStatement("update pair set pgkbCALevel=? where pairid=?");
      closables.add(updateLevelStmt);
      updateTestingStmt = conn.prepareStatement("update pair set pgxTesting=? where pairid=?");
      closables.add(updateTestingStmt);
    }

    private int update(Long pairId, String level, String testing) throws SQLException {
      int changes = 0;
      if (level != null) {
        updateLevelStmt.setString(1, level);
        updateLevelStmt.setLong(2, pairId);
        changes += updateLevelStmt.executeUpdate();
      }
      if (testing != null) {
        updateTestingStmt.setString(1, testing);
        updateTestingStmt.setLong(2, pairId);
        changes += updateTestingStmt.executeUpdate();
      }
      return changes;
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
