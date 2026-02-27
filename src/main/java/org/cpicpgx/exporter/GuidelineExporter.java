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
import java.util.ArrayList;
import java.util.List;

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
            "select " +
                "g.name \"Name\", " +
                "g.clinpgxid \"ClinPGx Id\", " +
                "g.genes \"Genes\", " +
                "array_agg(distinct d.name order by d.name) as \"Drugs\", " +
                "array_agg(distinct p.pmid order by p.pmid) as \"PMIDs\", " +
                "g.notesonusage as \"Notes on Usage\", " +
                "g.id::text as \"ID\" " +
                "from guideline g " +
                "    left join publication p on g.id = p.guidelineid " +
                "    left join drug d on g.id = d.guidelineid " +
                "group by g.name, g.clinpgxId, g.genes, g.notesonusage, g.id " +
                "order by g.name")
    ) {
      try (ResultSet rs = changeStmt.executeQuery()) {
        GuidelineWorkbook workbook = new GuidelineWorkbook();
        workbook.writeHeader(rs.getMetaData());
        List<String> ids = new ArrayList<>();

        while (rs.next()) {
          workbook.write(
              rs.getString(1), // name
              rs.getString(2), // clinpgx id
              rs.getArray(3),  // gene symbols
              rs.getString(7), // notes on usage
              rs.getArray(5),  // PMIDs
              rs.getArray(4)   // drug names
          );
          ids.add(rs.getString(8)); // guideline ID
        }

        workbook.writeChangeLog(queryChangeLog(conn, FileType.GUIDELINE));
        addFileExportHistory(workbook.getFilename(), ids.toArray(new String[0]));
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
