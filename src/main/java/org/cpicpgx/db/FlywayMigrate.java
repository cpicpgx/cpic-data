package org.cpicpgx.db;

import org.apache.commons.cli.*;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * This class will apply all the Flyway configuation DDL scripts to the database
 */
public class FlywayMigrate {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("r", false, "flag to reset (clean) the DB before migration");
    CommandLineParser clParser = new DefaultParser();

    try {
      CommandLine cli = clParser.parse(options, args);

      Flyway flyway = Flyway.configure().dataSource(
          ConnectionFactory.getJdbcUrl(),
          ConnectionFactory.getUser(),
          ConnectionFactory.getPass()
      ).schemas(ConnectionFactory.getSchema()).load();

      if (cli.hasOption("r")) {
        sf_logger.info("Cleaning DB, as requested");
        flyway.clean();
      }

      if (flyway.info().current() == null) {
        sf_logger.info("Starting fresh");
      } else {
        sf_logger.info("Starting at version {}", flyway.info().current().getVersion());
      }
      int migrations = flyway.migrate();
      sf_logger.info("Applied {} changes, now at version {}", migrations, flyway.info().current().getVersion());
    }
    catch (ParseException ex) {
      sf_logger.error("Error parsing args", ex);
    }
  }
}
