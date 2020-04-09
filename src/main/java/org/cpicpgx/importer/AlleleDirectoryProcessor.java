package org.cpicpgx.importer;

import org.cpicpgx.db.NoteType;
import org.cpicpgx.model.EntityType;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.WorkbookWrapper;

import java.sql.SQLException;

/**
 * This class crawls the given directory for <code>.xlsx</code> files and runs the {@link AlleleDefinitionImporter} on 
 * each one in succession.
 *
 * @author Ryan Whaley
 */
public class AlleleDirectoryProcessor extends BaseDirectoryImporter {
  private static final String[] sf_deleteStatements = new String[]{
      "delete from gene_note where type='" + NoteType.ALLELE_DEFINITION.name() + "'",
      "delete from allele_location_value",
      "delete from allele_definition where geneSymbol not in ('HLA-A','HLA-B')",
      "delete from sequence_location"
  };
  private static final String DEFAULT_DIRECTORY = "allele_definition_tables";

  public static void main(String[] args) {
    rebuild(new AlleleDirectoryProcessor(), args);
  }

  public AlleleDirectoryProcessor() { }

  @Override
  public String getDefaultDirectoryName() {
    return DEFAULT_DIRECTORY;
  }

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
    return EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) {
    try {
      AlleleDefinitionImporter importer = new AlleleDefinitionImporter(workbook);
      importer.writeToDB();
      writeNotes(EntityType.GENE, importer.getGene(), workbook.getNotes());
      importer.writeHistory(workbook);
      addImportHistory(workbook);
    } catch (SQLException e) {
      throw new RuntimeException("Error processing " + workbook, e);
    }
  }
}
