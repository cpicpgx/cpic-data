package org.cpicpgx.exporter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;

public class ChangelogExporter extends BaseExporter {
    private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FILE_NAME = "data.tsv";
    private static final String SQL_STMT = "select date as \"Date of Change\", type as \"Type of Data\", entityname as \"Subject\", note as \"Note of Change\" from change_log_view order by date desc, type, entityname, note";

    public static void main(String[] args) {
        ChangelogExporter exporter = new ChangelogExporter();
        try {
            exporter.parseArgs(args);
            exporter.export();
        } catch (Exception e) {
            sf_logger.error("Error exporting changelog", e);
        }
    }

    @Override
    public FileType getFileType() {
        return FileType.CHANGELOG;
    }

    @Override
    public void export() throws Exception {
        Path filePath = this.directory.resolve(FILE_NAME);
        try (
                FileWriter fileWriter = new FileWriter(filePath.toFile());
                Connection conn = ConnectionFactory.newConnection();
                ResultSet rs = conn.prepareStatement(SQL_STMT).executeQuery();
                CSVPrinter printer = CSVFormat.TDF.builder()
                        .setHeader(rs)
                        .setQuote(null)
                        .setEscape(null)
                        .setRecordSeparator("\n")
                        .build().print(fileWriter)
        ) {
            printer.printRecords(rs);
        }
    }
}
