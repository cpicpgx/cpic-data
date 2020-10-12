package org.cpicpgx.importer;

import org.cpicpgx.model.FileType;
import org.cpicpgx.util.Constants;
import org.cpicpgx.util.WorkbookWrapper;

import java.sql.SQLException;

/**
 * This class crawls the given directory for <code>.xlsx</code> files and runs the {@link AlleleDefinitionImporter} on 
 * each one in succession.
 *
 * @author Ryan Whaley
 */
public class AlleleDirectoryProcessor extends BaseDirectoryImporter {
  //language=PostgreSQL
  private static final String[] sf_deleteStatements = new String[]{
      "delete from change_log where type='" + FileType.ALLELE_DEFINITION.name() + "'",
      "delete from file_note where type='" + FileType.ALLELE_DEFINITION.name() + "'",
      "delete from allele where not genesymbol ~ '^HLA'",
      "delete from allele_location_value",
      "delete from allele_definition where geneSymbol not in ('HLA-A','HLA-B')",
      "delete from sequence_location"
  };

  public static void main(String[] args) {
    rebuild(new AlleleDirectoryProcessor(), args);
  }

  public AlleleDirectoryProcessor() { }

  @Override
  public FileType getFileType() {
    return FileType.ALLELE_DEFINITION;
  }
  
  @Override
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  String getFileExtensionToProcess() {
    return Constants.EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) {
    try {
      AlleleDefinitionImporter importer = new AlleleDefinitionImporter(workbook);
      importer.writeToDB();
      writeNotes(importer.getGene(), workbook.getNotes());
      importer.writeHistory(workbook);
    } catch (SQLException e) {
      throw new RuntimeException("Error processing " + workbook, e);
    }
  }
}
