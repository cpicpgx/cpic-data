package org.cpicpgx.exporter;

import org.apache.commons.cli.ParseException;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.cpicpgx.workbook.GuidelineWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GuidelineExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    try {
      GuidelineExporter guidelineExporter = new GuidelineExporter();
      guidelineExporter.parseArgs(args);
      guidelineExporter.export();
    } catch (ParseException e) {
      sf_logger.error("Cound not parse command", e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public FileType getFileType() {
    return FileType.GUIDELINE;
  }

  public void export() {
    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement changeStmt = conn.prepareStatement(
            "select g.name \"Name\", g.url \"URL\",\n" +
                "       g.pharmgkbid \"PharmGKB Annotation IDs\",\n" +
                "       g.genes \"Genes\", g.notesonusage as \"Notes on Usage\",\n" +
                "       array_agg(distinct p.pmid order by p.pmid) as \"PMIDs\",\n" +
                "       array_agg(distinct d.name order by d.name) as \"Drugs\"\n" +
                "from guideline g\n" +
                "    left join publication p on g.id = p.guidelineid\n" +
                "    left join drug d on g.id = d.guidelineid\n" +
                "group by g.name, g.url, g.pharmgkbid, g.genes, g.notesonusage\n" +
                "order by g.name")
    ) {
      try (ResultSet rs = changeStmt.executeQuery()) {
        GuidelineWorkbook workbook = new GuidelineWorkbook();
        workbook.writeHeader(rs.getMetaData());

        while (rs.next()) {
          workbook.write(
              rs.getString(1), // name
              rs.getString(2), // url
              rs.getArray(3),  // pharmgkb annotation ids
              rs.getArray(4),  // gene symbols
              rs.getString(5), // notes on usage
              rs.getArray(6),  // PMIDs
              rs.getArray(7)   // drug names
          );
        }

        workbook.writeChangeLog(queryChangeLog(conn, FileType.GUIDELINE));
        addFileExportHistory(workbook.getFilename(), new String[]{});
        writeWorkbook(workbook);
        handleFileUpload();
      }
    } catch (IOException e) {
      sf_logger.error("Couldn't write to filesystem", e);
    } catch (SQLException e) {
      sf_logger.error("Couldn't query the DB", e);
    } catch (Exception e) {
      sf_logger.error("Error making pairs", e);
    }
  }
}
