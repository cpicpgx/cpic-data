package org.cpicpgx.db;

import org.apache.commons.cli.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
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
    options.addOption("p", "repair", false, "flag to repair the migration hashes");
    CommandLineParser clParser = new DefaultParser();

    try {
      CommandLine cli = clParser.parse(options, args);

      Flyway flyway = Flyway.configure().dataSource(
          ConnectionFactory.getJdbcUrl(),
          ConnectionFactory.getUser(),
          ConnectionFactory.getPass()
      ).schemas(ConnectionFactory.getSchema()).load();

      if (cli.hasOption("p")) {
        sf_logger.info("Repairing migration hashes");
        flyway.repair();
      }

      if (cli.hasOption("r")) {
        sf_logger.info("Cleaning DB, as requested");
        flyway.clean();
      }

      if (flyway.info().current() == null) {
        sf_logger.info("Starting fresh");
      } else {
        sf_logger.info("Starting at version {}", flyway.info().current().getVersion());
      }
      MigrateResult result = flyway.migrate();
      sf_logger.info("Applied {} changes, now at version {}", result.migrationsExecuted, flyway.info().current().getVersion());
    }
    catch (ParseException ex) {
      sf_logger.error("Error parsing args", ex);
    }
  }
}
