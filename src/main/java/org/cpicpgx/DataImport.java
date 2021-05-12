package org.cpicpgx;

import org.apache.commons.cli.*;
import org.cpicpgx.importer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  private final Path m_directory;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("d", true,"directory that has sub-folders with excel data files (*.xlsx)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      DataImport processor = new DataImport(cli.getOptionValue("d"));
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

  private void execute() throws Exception {

    // order is important in this list, later importers may add data to previously imported data
    List<BaseDirectoryImporter> importers = new ArrayList<>();
    importers.add(new GeneReferenceImporter().setDirectory(m_directory, null));
    importers.add(new DrugImporter().setDirectory(m_directory, null));
    importers.add(new PairImporter().setDirectory(m_directory, null));
    importers.add(new AlleleDefinitionImporter().setDirectory(m_directory, null));
    importers.add(new FunctionReferenceImporter().setDirectory(m_directory, null));
    importers.add(new GenePhenotypeImporter().setDirectory(m_directory, null));
    importers.add(new GeneCdsImporter().setDirectory(m_directory, null));
    importers.add(new AlleleFrequencyImporter().setDirectory(m_directory, null));
    importers.add(new RecommendationImporter().setDirectory(m_directory, null));
    importers.add(new TestAlertImporter().setDirectory(m_directory, null));
    
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

    // load PharmVar data
    PharmVarApiImporter.execute();
  }
}
