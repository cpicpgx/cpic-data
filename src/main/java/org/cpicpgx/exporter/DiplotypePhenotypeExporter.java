package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.model.FileType;
import org.cpicpgx.workbook.DiplotypeWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sawano.java.text.AlphanumericComparator;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exports files, by gene, of the diplotype to phenotype assignments.
 *
 * @author Ryan Whaley
 */
public class DiplotypePhenotypeExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    DiplotypePhenotypeExporter exporter = new DiplotypePhenotypeExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error diplotype phenotype", ex);
    }
  }

  public FileType getFileType() {
    return FileType.DIPLOTYPE_PHENOTYPE;
  }

  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement("select distinct d.geneSymbol, g.lookupmethod from diplotype d join gene g on (d.genesymbol=g.symbol) where d.ehrpriority is not null order by 1");
         PreparedStatement dipStmt = conn.prepareStatement("select d.diplotype, d.generesult, d.ehrpriority, d.totalactivityscore from diplotype d where d.genesymbol=? order by d.diplotype");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String gene = grs.getString(1);
        LookupMethod lookupMethod = LookupMethod.valueOf(grs.getString(2));
        if (lookupMethod == LookupMethod.ALLELE_STATUS) continue;

        DiplotypeWorkbook workbook = new DiplotypeWorkbook(gene);

        dipStmt.setString(1, gene);
        Map<String, String[]> dipMap = new TreeMap<>(new AlphanumericComparator(Locale.ENGLISH));
        try (ResultSet rs = dipStmt.executeQuery()) {
          while (rs.next()) {
            String diplotype = rs.getString(1);
            String phenotype = rs.getString(2);
            String ehr = rs.getString(3);
            String activity = rs.getString(4);
            dipMap.put(diplotype, new String[]{diplotype, gene + " " + phenotype, ehr, activity});
          }
        }
        for (String key : dipMap.keySet()) {
          String[] fields = dipMap.get(key);
          workbook.writeDiplotype(fields[0], fields[1], fields[2], fields[3]);
        }

        workbook.writeNotes(queryNotes(conn, gene, FileType.DIPLOTYPE_PHENOTYPE));

        workbook.writeGenerated();

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{gene});
      }
      handleFileUpload();
    }
  }
}
