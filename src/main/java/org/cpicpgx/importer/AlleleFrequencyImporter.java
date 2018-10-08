package org.cpicpgx.importer;

import org.apache.commons.cli.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.cpicpgx.util.WorkbookWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Class to read all excel files in the given directory and store the allele frequency information found in them.
 * 
 * Excel file names are expected to be snake_cased and have the gene symbol as the first word in the filename.
 *
 * @author Ryan Whaley
 */
public class AlleleFrequencyImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  private Path m_directory;

  public static void main(String[] args) {
    try {
      Options options = new Options();
      options.addOption("d", true,"directory containing allele frequency excel files (*.xlsx)");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);

      AlleleFrequencyImporter processor = new AlleleFrequencyImporter(Paths.get(cli.getOptionValue("d")));
      processor.execute();
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
    }
  }

  /**
   * Constructor
   * @param directoryPath an existing directory containing Excel .xlsx files
   */
  public AlleleFrequencyImporter(Path directoryPath) {
    if (directoryPath == null) {
      throw new IllegalArgumentException("No directory given");
    }

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

  /**
   * Read the files and store the data in the database
   */
  public void execute() {
    Arrays.stream(Objects.requireNonNull(m_directory.toFile().listFiles()))
        .filter(f -> f.getName().toLowerCase().endsWith(".xlsx") && !f.getName().startsWith("~$"))
        .forEach(processFile);
  }

  private Consumer<File> processFile = (File file) -> {
    sf_logger.info("Reading {}", file);

    String[] nameParts = file.getName().split("_");
    
    try (InputStream in = Files.newInputStream(file.toPath())) {
      processAlleles(new WorkbookWrapper(in), nameParts[0]);
    } catch (IOException| InvalidFormatException ex) {
      throw new RuntimeException("Error processing frequency file", ex);
    }
  };

  /**
   * Finds the sheet with allele data and iterates through the rows with data.
   * The session is auto-committed so no explict commit is done here.
   * @param workbook The workbook to read
   * @param gene The symbol of the gene the alleles in this workbook are for
   */
  private void processAlleles(WorkbookWrapper workbook, String gene) {
    workbook.currentSheetIs("References");
    
    try (FrequencyProcessor frequencyProcessor = new FrequencyProcessor(gene, workbook.getRow(0))) {
      for (int i = 1; i < workbook.currentSheet.getLastRowNum(); i++) {
        try {
          frequencyProcessor.insertPopulation(workbook.getRow(i));
        } catch (Exception ex) {
          throw new RuntimeException("Error parsing row " + (i+1), ex);
        }
      }
      sf_logger.info("Successfully parsed " + gene + " frequencies from " + workbook);
    } catch (Exception ex) {
      sf_logger.error("Error saving to DB", ex);
    }
  }
}
