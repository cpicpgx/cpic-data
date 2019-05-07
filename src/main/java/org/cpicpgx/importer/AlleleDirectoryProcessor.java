package org.cpicpgx.importer;

import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

/**
 * This class crawls the given directory for <code>.xlsx</code> files and runs the {@link AlleleDefinitionImporter} on 
 * each one in succession.
 *
 * @author Ryan Whaley
 */
public class AlleleDirectoryProcessor extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String[] sf_deleteStatements = new String[]{
      "delete from translation_note",
      "delete from allele_location_value",
      "delete from allele",
      "delete from sequence_location"
  };
  private static final String DEFAULT_DIRECTORY = "allele_definition_tables";

  public static void main(String[] args) {
    rebuild(new AlleleDirectoryProcessor(), args);
  }

  public AlleleDirectoryProcessor() { }

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
