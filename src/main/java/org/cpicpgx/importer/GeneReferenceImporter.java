package org.cpicpgx.importer;

import org.cpicpgx.model.FileType;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parses gene mapping files for NCBI, HGNC, and Ensembl data. Those IDs are then set in the gene table
 *
 * @author Ryan Whaley
 */
public class GeneReferenceImporter extends BaseDirectoryImporter {
  private static final String[] sf_deleteStatements = new String[]{};

  public static void main(String[] args) {
    rebuild(new GeneReferenceImporter(), args);
  }
  
  public GeneReferenceImporter() {}
  
  String[] getDeleteStatements() {
    return sf_deleteStatements;
  }

  @Override
  public FileType getFileType() {
    return FileType.GENE_RESOURCE;
  }

  @Override
  String getFileExtensionToProcess() {
    return EXCEL_EXTENSION;
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    try (GeneDbHarness dbHarness = new GeneDbHarness()) {
      String geneSymbol = null;
      String hgncId = null;
      String ncbiId = null;
      String ensemblId = null;

      for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        String idType = row.getNullableText(2);
        String idValue = row.getNullableText(3, true);
        geneSymbol = row.getNullableText(0);

        if (idValue != null) {
          switch (idType) {
            case "HGNC ID":
              hgncId = idValue;
              break;
            case "Gene ID":
              ncbiId = idValue;
              break;
            case "Ensembl ID":
              ensemblId = idValue;
              break;
            default:
              // fall out
          }
        }
      }

      dbHarness.upsert(geneSymbol, hgncId, ncbiId, ensemblId);
    }
  }

  private static class GeneDbHarness extends DbHarness {
    private final PreparedStatement upsertGene;

    GeneDbHarness() throws SQLException {
      super(FileType.GENE_RESOURCE);
      //language=PostgreSQL
      upsertGene = prepare("insert into gene(symbol, hgncid, ncbiid, ensemblid) values (?,?,?,?) " +
          "on conflict (symbol) do update " +
          "set hgncId=excluded.hgncid, ncbiId=excluded.ncbiid, ensemblId=excluded.ensemblid");
    }

    private void upsert(String geneSymbol, String hgncId, String ncbiId, String ensemblId) throws SQLException {
      this.upsertGene.clearParameters();
      this.upsertGene.setString(1, geneSymbol);
      setNullableString(this.upsertGene, 2, hgncId);
      setNullableString(this.upsertGene, 3, ncbiId);
      setNullableString(this.upsertGene, 4, ensemblId);
      this.upsertGene.executeUpdate();
    }
  }
}
