package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
  
  private void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select g.symbol, g.functionalityreferencelastmodified from gene g where g.functionalityreferencelastmodified is not null order by 1");
         PreparedStatement alleleStmt = conn.prepareStatement("select a.name, a.id from allele a where exists(select 1 from function_reference fr where a.id = fr.alleleid) and a.genesymbol=? order by 2");
         PreparedStatement fxnStmt = conn.prepareStatement("select f.pmid, f.finding, f.allele_function, f.substrate_in_vitro, f.substrate_in_vivo from function_reference f where f.alleleid=?");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String symbol = grs.getString(1);
        Date lastModified = grs.getDate(2);
        
        AlleleFunctionalityReferenceWorkbook workbook = new AlleleFunctionalityReferenceWorkbook(symbol, lastModified);
        
        alleleStmt.setString(1, symbol);
        try (ResultSet rs = alleleStmt.executeQuery()) {
          while (rs.next()) {
            String alleleName = rs.getString(1);
            int alleleId = rs.getInt(2);

            fxnStmt.setInt(1, alleleId);
            try (ResultSet frs = fxnStmt.executeQuery()) {
              while (frs.next()) {
                String pmid = frs.getString(1);
                String finding = frs.getString(2);
                String function = frs.getString(3);

                Array subInVitro = frs.getArray(4);
                List<String> subInVitroList = new ArrayList<>();
                if (subInVitro != null) {
                  subInVitroList.addAll(Arrays.asList((String[])subInVitro.getArray()));
                }

                Array subInVivo = frs.getArray(5);
                List<String> subInVivoList = new ArrayList<>();
                if (subInVivo != null) {
                  subInVivoList.addAll(Arrays.asList((String[])subInVivo.getArray()));
                }

                workbook.writeAlleleRow(alleleName, function, pmid, finding, subInVitroList, subInVivoList);
              }
            }
          }
        }

        writeWorkbook(workbook);
      }
    }
  }
}
