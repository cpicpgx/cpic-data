package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

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

  /**
   * Export all genes with allele definitions, 1 file per gene.
   * @throws Exception can occur from IO or database activity
   */
  private void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select distinct a.hgncid, g.alleleslastmodified from allele a join gene g on a.hgncid = g.hgncid order by 1");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String symbol = grs.getString(1);
        Date allelesLastModified = grs.getDate(2);
      
        AlleleDefinitionWorkbook workbook = new AlleleDefinitionWorkbook(symbol, allelesLastModified);

        try (PreparedStatement seqLocStmt = conn.prepareStatement("select name, proteinlocation, chromosomelocation, genelocation, dbsnpid, id from sequence_location where hgncid=?")) {
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
            PreparedStatement alleleStmt = conn.prepareStatement("select name, functionalstatus, id from allele where hgncid=?");
            PreparedStatement locValStmt = conn.prepareStatement("select locationid, variantallele from allele_location_value where alleleid=?")
        ) {
          alleleStmt.setString(1, symbol);
          try (ResultSet rs = alleleStmt.executeQuery()) {
            while (rs.next()) {
              long alleleId = rs.getLong(3);
              workbook.writeAllele(rs.getString(1), rs.getString(2));

              locValStmt.setLong(1, alleleId);
              try (ResultSet vrs = locValStmt.executeQuery()) {
                while (vrs.next()) {
                  workbook.writeAlleleLocationValue(vrs.getLong(1), vrs.getString(2));
                }
              }
            }
          }
        }

        Path filePath = this.directory.resolve(workbook.getFilename());
        try (OutputStream out = Files.newOutputStream(filePath)) {
          workbook.write(out);
        }
        sf_logger.info("Wrote definitions for {} to {}", symbol, filePath);
      }
    }
  }
}
