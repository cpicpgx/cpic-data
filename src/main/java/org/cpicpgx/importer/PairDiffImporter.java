package org.cpicpgx.importer;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
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
import java.sql.Types;
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
          if (pairDiff.pairId != null) {
            Arrays.stream(pairDiff.messages).forEach(sf_logger::info);
            int changes = dbHarness.update(pairDiff.pairId, pairDiff.pgkbCaLevel, pairDiff.pgxTesting);
            sf_logger.info("{} changes applied to {}", changes, pairDiff.name);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Error reading diffs", e);
      }
    };
  }
  
  private static class PairDiff {
    String[] messages;
    String name;
    Long pairId;
    String pgkbCaLevel;
    String pgxTesting;
  }

  private static class DbHarness implements AutoCloseable {
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final PreparedStatement updateLevelStmt;
    private final PreparedStatement updateTestingStmt;

    DbHarness() throws SQLException {
      Connection conn = ConnectionFactory.newConnection();
      closeables.add(conn);

      updateLevelStmt = conn.prepareStatement("update pair set pgkbCALevel=? where pairid=?");
      closeables.add(updateLevelStmt);
      updateTestingStmt = conn.prepareStatement("update pair set pgxTesting=? where pairid=?");
      closeables.add(updateTestingStmt);
    }

    private int update(Long pairId, String level, String testing) throws SQLException {
      int changes = 0;
      if (level != null) {
        if (StringUtils.isBlank(level)) {
          updateLevelStmt.setNull(1, Types.VARCHAR);
        }
        else {
          updateLevelStmt.setString(1, level);
        }
        updateLevelStmt.setLong(2, pairId);
        changes += updateLevelStmt.executeUpdate();
      }
      if (testing != null) {
        if (StringUtils.isBlank(testing)) {
          updateTestingStmt.setNull(1, Types.VARCHAR);
        }
        else {
          updateTestingStmt.setString(1, testing);
        }
        updateTestingStmt.setLong(2, pairId);
        changes += updateTestingStmt.executeUpdate();
      }
      return changes;
    }

    @Override
    public void close() {
      closeables.forEach(c -> {
        try {
          c.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
