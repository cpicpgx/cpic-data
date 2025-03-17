package org.cpicpgx.stats;

import org.apache.commons.cli.*;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.stats.model.StatisticType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class StatisticsReport {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  Date dateGathered;
  SortedMap<StatisticType, Long> stats = new TreeMap<>();

  public static void main(String[] args) {

    Options options = new Options();
    try {
      options.addOption(
          Option.builder("o")
              .argName("output-dir")
              .desc("directory to write report to")
              .hasArg()
              .numberOfArgs(1)
              .required()
              .build()
      );
      CommandLineParser parser = new DefaultParser();
      CommandLine cl = parser.parse(options, args);

      StatisticsReport statisticsReport = new StatisticsReport();
      Path outputPath = statisticsReport.write(cl.getOptionValue("o"));
      System.out.println("Wrote " + outputPath.toAbsolutePath());

    } catch (SQLException e) {
      sf_logger.error("Error working with the database", e);
      System.exit(1);
    } catch (IOException e) {
      sf_logger.error("Error writing output", e);
      System.exit(1);
    } catch (ParseException e) {
      sf_logger.error("Error parsing command line arguments", e);
      System.exit(1);
    }
  }

  StatisticsReport() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      try (ResultSet rs = conn.prepareStatement("select createdon, stattype, statvalue from statistic where createdon=(select max(createdon) from statistic)")
          .executeQuery()) {
        while (rs.next()) {
          if (dateGathered == null) {
            dateGathered = rs.getDate("createdon");
          }
          stats.put(StatisticType.valueOf(rs.getString("stattype")), rs.getLong("statvalue"));
        }
      }
    }
  }

  Path write(String dirPathString) throws IOException {
    Path outputPath = Paths.get(dirPathString).resolve("statistics-" + dateGathered + ".md");

    List<String> groups = Arrays.stream(StatisticType.values()).map(StatisticType::getGroup).distinct().toList();

    try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
      writer.write("# CPIC Statistics");
      writer.newLine();
      writer.newLine();
      writer.write("For data gathered on: " + dateGathered.toString());
      writer.newLine();
      writer.newLine();

      for (String group : groups) {
        writer.write("## " + group);
        writer.newLine();
        writer.newLine();
        writer.write("| Stat | Count |");
        writer.newLine();
        writer.write("| :--- | ---: |");
        writer.newLine();
        for (StatisticType type : Arrays.stream(StatisticType.values()).filter(type -> group.equals(type.getGroup())).toList()) {
          writer.write("| " + type.getDescription() + " | " + stats.get(type) + " |");
          writer.newLine();
        }
        writer.newLine();
        writer.newLine();
      }
    }
    return outputPath;
  }
}
