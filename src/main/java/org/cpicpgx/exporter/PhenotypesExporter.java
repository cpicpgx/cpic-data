package org.cpicpgx.exporter;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.ObjectUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.ActivityScoreComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Export gene phenotypes excel workbooks
 *
 * @author Ryan Whaley
 */
public class PhenotypesExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    PhenotypesExporter exporter = new PhenotypesExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting gene phenotypes", ex);
    }
  }

  public FileType getFileType() {
    return FileType.GENE_PHENOTYPE;
  }

  public void export() throws Exception {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement stmt = conn.prepareStatement("select distinct p.genesymbol, gene.lookupmethod from gene_result p join gene_result_lookup pf on p.id = pf.phenotypeid join gene on p.genesymbol = gene.symbol where p.result is not null and (pf.function1 is not null or pf.activityvalue1 is not null) order by 1");
        ResultSet rs = stmt.executeQuery();
        PreparedStatement pstmt = conn.prepareStatement("select function1, function2, activityvalue1, activityvalue2, totalactivityscore, result, description\n" +
            "from gene_result p join gene_result_lookup pf on p.id = pf.phenotypeid\n" +
            "where p.genesymbol=?")
    ) {
      while (rs.next()) {
        String gene = rs.getString(1);
        LookupMethod lookupMethod = LookupMethod.valueOf(rs.getString(2));
        Comparator<String> keyComparator = lookupMethod == LookupMethod.ACTIVITY_SCORE ? ActivityScoreComparator.getComparator() : String.CASE_INSENSITIVE_ORDER.reversed();
        int keyIndex = lookupMethod == LookupMethod.ACTIVITY_SCORE ? 4 : 5;

        PhenotypesWorkbook phenotypesWorkbook = new PhenotypesWorkbook(gene);

        pstmt.setString(1, gene);
        try (ResultSet pRs = pstmt.executeQuery()) {
          List<List<String>> results = new ArrayList<>();
          while (pRs.next()) {
            results.add(ImmutableList.of(
                ObjectUtils.defaultIfNull(pRs.getString(1), ""),
                ObjectUtils.defaultIfNull(pRs.getString(2), ""),
                ObjectUtils.defaultIfNull(pRs.getString(3), ""),
                ObjectUtils.defaultIfNull(pRs.getString(4), ""),
                ObjectUtils.defaultIfNull(pRs.getString(5), ""),
                ObjectUtils.defaultIfNull(pRs.getString(6), ""),
                ObjectUtils.defaultIfNull(pRs.getString(7), "")
            ));
          }
          List<String> sortKeys = results.stream().map((r) -> r.get(keyIndex)).sorted(keyComparator).distinct().collect(Collectors.toList());
          for (String key : sortKeys) {
            results.stream().filter(r -> r.get(keyIndex).equals(key)).forEach(r ->
                phenotypesWorkbook.writePhenotype(r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6))
            );
          }
        }

        phenotypesWorkbook.writeChangeLog(queryChangeLog(conn, gene, getFileType()));

        writeWorkbook(phenotypesWorkbook);
        addFileExportHistory(phenotypesWorkbook.getFilename(), new String[]{gene});
      }
      handleFileUpload();
    }
  }
}
