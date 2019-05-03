package org.cpicpgx.importer;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * This class crawls the given directory for <code>.xlsx</code> files and runs the {@link AlleleDefinitionImporter} on 
 * each one in succession.
 *
 * @author Ryan Whaley
 */
public class AlleleDirectoryProcessor extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      AlleleDirectoryProcessor processor = new AlleleDirectoryProcessor();
      processor.parseArgs(args);
      processor.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }

  private AlleleDirectoryProcessor() { }

  public AlleleDirectoryProcessor(Path directory) {
    this.setDirectory(directory);
  }

  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) {
    try {
      AlleleDefinitionImporter importer = new AlleleDefinitionImporter(workbook);
      importer.writeToDB();
      sf_logger.info("Processed {}", workbook);
    } catch (SQLException e) {
      throw new RuntimeException("Error processing " + workbook, e);
    }
  }
}
