package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Write allele definitions out to Excel XLSX files, one per gene.
 *
 * @author Ryan Whaley
 */
public class AlleleDefinitionExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Main executable, needs a -d argument to specify the directory to write to
   * @param args the CLI arguments
   */
  public static void main(String[] args) {
    AlleleDefinitionExporter exporter = new AlleleDefinitionExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting allele definitions", ex);
    }
  }

  public FileType getFileType() {
    return FileType.ALLELE_DEFINITION;
  }

  /**
   * Export all genes with allele definitions, 1 file per gene.
   * @throws Exception can occur from IO or database activity
   */
  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select g.symbol, g.chromosequenceid, g.proteinsequenceid, g.genesequenceid, g.mrnasequenceid, sum(case when a.pharmvarid is null then 0 else 1 end) pvIds from allele_definition a join gene g on a.geneSymbol = g.symbol join allele_location_value alv on a.id = alv.alleledefinitionid\n" +
             "group by g.symbol, g.chromosequenceid, g.proteinsequenceid, g.genesequenceid, g.mrnasequenceid\n" +
             "order by 1");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String symbol = grs.getString(1);
        String seqChr = grs.getString(2);
        String seqPro = grs.getString(3);
        String seqGen = grs.getString(4);
        String seqMrna = grs.getString(5);
        Long pvCount = grs.getLong(6);
      
        AlleleDefinitionWorkbook workbook = new AlleleDefinitionWorkbook(symbol, seqChr, seqPro, seqGen, seqMrna, pvCount);

        try (PreparedStatement seqLocStmt = conn.prepareStatement("select name, proteinlocation, chromosomelocation, genelocation, dbsnpid, id from sequence_location where geneSymbol=?")) {
          seqLocStmt.setString(1, symbol);
          try (ResultSet rs = seqLocStmt.executeQuery()) {
            while (rs.next()) {
              workbook.writeVariant(
                  rs.getString(1), 
                  rs.getString(2), 
                  rs.getString(3),
                  rs.getString(4),
                  rs.getString(5),
                  rs.getLong(6)
              );
            }
          }
        }
      
        try (
            PreparedStatement alleleStmt = conn.prepareStatement("select name, id from allele_definition where geneSymbol=?");
            PreparedStatement locValStmt = conn.prepareStatement("select locationid, variantallele from allele_location_value where alleledefinitionid=?")
        ) {
          alleleStmt.setString(1, symbol);
          try (ResultSet rs = alleleStmt.executeQuery()) {
            
            // parse to an intermediary map so we can sort the allele names properly
            SortedMap<String, Long> alleleMap = new TreeMap<>(HaplotypeNameComparator.getComparator()); 
            while (rs.next()) {
              alleleMap.put(rs.getString(1), rs.getLong(2));
            }
            
            for (String alleleName : alleleMap.keySet()) {
              Long alleleId = alleleMap.get(alleleName);
              workbook.writeAllele(alleleName);
              locValStmt.setLong(1, alleleId);
              try (ResultSet vrs = locValStmt.executeQuery()) {
                while (vrs.next()) {
                  workbook.writeAlleleLocationValue(vrs.getLong(1), vrs.getString(2));
                }
              }
            }
          }
        }

        workbook.writeNotes(queryNotes(conn, symbol, FileType.ALLELE_DEFINITION));

        workbook.writeChangeLog(queryChangeLog(conn, symbol, getFileType()));

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{symbol});
      }
      handleFileUpload();
    }
  }
}
