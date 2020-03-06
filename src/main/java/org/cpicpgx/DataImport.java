package org.cpicpgx;

import org.apache.commons.cli.*;
import org.cpicpgx.importer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An import task for getting all information from excel workbooks in the proper order and then inserting it into the 
 * database.
 *
 * @author Ryan Whaley
 */
public class DataImport {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String alleleDirectory = null;
  private String frequencyDirectory = null;
  private String functionDirectory = null;
  private String recommendationDirectory = null;
  private String geneMappingDirectory = null;
  private String testAlertsDirectory = null;
  private String geneCdsDirectory = null;

  private Path m_directory;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("d", true,"directory that has sub-folders with excel data files (*.xlsx)");
      options.addOption("ad", true,"allele definition subdirectory name");
      options.addOption("fd", true,"allele frequency subdirectory name");
      options.addOption("rd", true,"function reference subdirectory name");
      options.addOption("dr", true,"recommendation subdirectory name");
      options.addOption("gm", true,"gene mapping subdirectory name");
      options.addOption("ta", true,"test alerts subdirectory name");
      options.addOption("gc", true,"gene CDS subdirectory name");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      DataImport processor = new DataImport(cli.getOptionValue("d"));
      processor.alleleDirectory         = cli.getOptionValue("ad");
      processor.frequencyDirectory      = cli.getOptionValue("fd");
      processor.functionDirectory       = cli.getOptionValue("rd");
      processor.recommendationDirectory = cli.getOptionValue("dr");
      processor.geneMappingDirectory    = cli.getOptionValue("gm");
      processor.testAlertsDirectory     = cli.getOptionValue("ta");
      processor.geneCdsDirectory        = cli.getOptionValue("gc");

        processor.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
      System.exit(1);
    } catch (Exception ex) {
      sf_logger.error("Error importing data", ex);
    }
  }

  private DataImport(String directory) {
    if (directory == null) {
      throw new IllegalArgumentException("No directory given");
    }
    Path directoryPath = Paths.get(directory);
    if (!directoryPath.toFile().exists()) {
      throw new IllegalArgumentException("Directory doesn't exist " + directoryPath);
    }
    if (!directoryPath.toFile().isDirectory()) {
      throw new IllegalArgumentException("Path is not a directory " + directoryPath);
    }
    if (directoryPath.toFile().listFiles() == null) {
      throw new IllegalArgumentException("Directory is empty " + directoryPath);
    }

    m_directory = directoryPath;
  }

  private void execute() throws SQLException {

    // order is important in this list, later importers may add data to previously imported data
    List<BaseDirectoryImporter> importers = new ArrayList<>();
    importers.add(new GeneReferenceImporter().setDirectory(m_directory, geneMappingDirectory));
    importers.add(new AlleleDirectoryProcessor().setDirectory(m_directory, alleleDirectory));
    importers.add(new PharmVarImporter().setDirectory(m_directory, null));
    importers.add(new AlleleFrequencyImporter().setDirectory(m_directory, frequencyDirectory));
    importers.add(new FunctionReferenceImporter().setDirectory(m_directory, functionDirectory));
    importers.add(new GenePhenotypeImporter().setDirectory(m_directory, recommendationDirectory));
    importers.add(new GeneCdsImporter().setDirectory(m_directory, geneCdsDirectory));
    importers.add(new RecommendationImporter().setDirectory(m_directory, recommendationDirectory));
    importers.add(new TestAlertImporter().setDirectory(m_directory, testAlertsDirectory));
    
    // reverse the importers before clearing data due to referential integrity
    Collections.reverse(importers);
    for (BaseDirectoryImporter importer : importers) {
      importer.clearAllData();
    }

    // reverse again to put them in the "right" order for loading
    Collections.reverse(importers);
    for (BaseDirectoryImporter importer : importers) {
      importer.execute();
    }
  }
}
