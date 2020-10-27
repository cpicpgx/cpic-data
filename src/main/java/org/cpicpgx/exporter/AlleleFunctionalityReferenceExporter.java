package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sawano.java.text.AlphanumericComparator;

import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class queries the functional_reference table and then dumps the contents out to excel workbooks
 *
 * @author Ryan Whaley
 */
public class AlleleFunctionalityReferenceExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    AlleleFunctionalityReferenceExporter exporter = new AlleleFunctionalityReferenceExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting allele functionality reference", ex);
    }
  }

  public FileType getFileType() {
    return FileType.ALLELE_FUNCTION_REFERENCE;
  }

  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement(
             "select distinct a.genesymbol from allele a where clinicalfunctionalstatus is not null order by 1");
         PreparedStatement alleleListStmt = conn.prepareStatement(
             "select name from allele where genesymbol=? and clinicalfunctionalstatus is not null");
         PreparedStatement alleleStmt = conn.prepareStatement(
             "select a.name, a.activityvalue, a.functionalstatus, a.clinicalfunctionalstatus, " +
                 "a.clinicalfunctionalsubstrate, a.citations, a.strength, a.findings, a.functioncomments " +
                 "from allele a where a.genesymbol=? and a.name=?");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String symbol = grs.getString(1);
        
        AlleleFunctionalityReferenceWorkbook workbook = new AlleleFunctionalityReferenceWorkbook(symbol);
        alleleListStmt.setString(1, symbol);
        SortedSet<String> alleleNames = new TreeSet<>(new AlphanumericComparator());
        try (ResultSet rs = alleleListStmt.executeQuery()) {
          while (rs.next()) {
            alleleNames.add(rs.getString(1));
          }
        }

        for (String alleleName : alleleNames) {
          alleleStmt.setString(1, symbol);
          alleleStmt.setString(2, alleleName);
          try (ResultSet rs = alleleStmt.executeQuery()) {
            if (rs.next()) {
              String activity = rs.getString(2);
              String function = rs.getString(3);
              String clinFunction = rs.getString(4);
              String clinSubstrate = rs.getString(5);
              Array citations = rs.getArray(6);
              String strength = rs.getString(7);
              String findings = rs.getString(8);
              String comments = rs.getString(9);

              String[] citationArray = citations == null ? new String[]{} : (String[]) citations.getArray();

              workbook.writeAlleleRow(alleleName, activity, function, clinFunction, clinSubstrate, citationArray, strength, findings, comments);
            } else {
              throw new RuntimeException("Allele name not found: " + alleleName);
            }
          }
        }
        
        workbook.writeNotes(queryNotes(conn, symbol, FileType.ALLELE_FUNCTION_REFERENCE));

        workbook.writeChangeLog(queryChangeLog(conn, symbol, getFileType()));

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{symbol});
      }
      handleFileUpload();
    }
  }
}
