package org.cpicpgx;

import org.apache.commons.cli.*;
import org.cpicpgx.importer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An import task for getting all information from excel workbooks in the proper order and then inserting it into the 
 * database.
 *
 * @author Ryan Whaley
 */
public class DataImport {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT_ALLELEDIRECTORY = "allele_definition_tables";
  private static final String DEFAULT_FREQUENCYDIRECTORY = "frequency_table";
  private static final String DEFAULT_FUNCTIONDIRECTORY = "allele_functionality_reference";
  private static final String DEFAULT_DIPLOTYPEDIRECTORY = "diplotype_phenotype_tables";
  private static final String DEFAULT_RECOMMENDATIONDIRECTORY = "recommendation_tables";
  private static final String DEFAULT_GENEMAPPINGDIRECTORY = "gene_resource_mappings";
  private static final String DEFAULT_TESTALERTDIRECTORY = "test_alerts";
  private static final String DEFAULT_GUIDELINESDIRECTORY = "guidelines";
  
  private String alleleDirectory = null;
  private String frequencyDirectory = null;
  private String functionDirectory = null;
  private String diplotypeDirectory = null;
  private String recommendationDirectory = null;
  private String geneMappingDirectory = null;
  private String testAlertsDirectory = null;
  private String guidelinesDirectory = null;

  private Path m_directory;

  public static void main(String[] args) {
    String directory = null;
    String allele = null;
    String frequency = null;
    String funcReference = null;
    String diplotype = null;
    String recommendation = null;
    String geneMapping = null;
    String testAlerts = null;
    String guidelines = null;
    try {
      Options options = new Options();
      options.addOption("d", true,"directory that has sub-folders with excel data files (*.xlsx)");
      options.addOption("ad", true,"allele definition subdirectory name");
      options.addOption("fd", true,"allele frequency subdirectory name");
      options.addOption("rd", true,"function reference subdirectory name");
      options.addOption("dd", true,"diplotype-phenotype subdirectory name");
      options.addOption("dr", true,"recommendation subdirectory name");
      options.addOption("gm", true,"gene mapping subdirectory name");
      options.addOption("ta", true,"test alerts subdirectory name");
      options.addOption("g", true,"guidelines subdirectory name");
      CommandLineParser clParser = new DefaultParser();
      CommandLine cli = clParser.parse(options, args);
      directory = cli.getOptionValue("d");

      allele = cli.getOptionValue("ad");
      frequency = cli.getOptionValue("fd");
      funcReference = cli.getOptionValue("rd");
      diplotype = cli.getOptionValue("dd");
      recommendation = cli.getOptionValue("dr");
      geneMapping = cli.getOptionValue("gm");
      testAlerts = cli.getOptionValue("ta");
      guidelines = cli.getOptionValue("g");
    } catch (ParseException e) {
      sf_logger.error("Couldn't parse command", e);
      System.exit(1);
    }

    DataImport processor = new DataImport(directory);
    processor.alleleDirectory = allele;
    processor.frequencyDirectory = frequency;
    processor.functionDirectory = funcReference;
    processor.diplotypeDirectory = diplotype;
    processor.recommendationDirectory = recommendation;
    processor.geneMappingDirectory = geneMapping;
    processor.testAlertsDirectory = testAlerts;
    processor.guidelinesDirectory = guidelines;
    processor.execute();
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

  private void execute() {
    new GuidelineImporter(m_directory.resolve(getGuidelinesDirectory())).execute();
    new GeneReferenceImporter(m_directory.resolve(getGeneMappingDirectory())).execute();
    new AlleleDirectoryProcessor(m_directory.resolve(getAlleleDirectory())).execute();
    new AlleleFrequencyImporter(m_directory.resolve(getFrequencyDirectory())).execute();
    new FunctionReferenceImporter(m_directory.resolve(getFunctionDirectory())).execute();
    new DiplotypePhenotypeImporter(m_directory.resolve(getDiplotypeDirectory())).execute();
    new RecommendationImporter(m_directory.resolve(getRecommendationDirectory())).execute();
    new TestAlertImporter(m_directory.resolve(getTestAlertsDirectory())).execute();
  }
  
  private String getAlleleDirectory() {
    if (this.alleleDirectory == null) {
      return DEFAULT_ALLELEDIRECTORY;
    } else {
      return this.alleleDirectory;
    }
  }
  
  private String getFrequencyDirectory() {
    if (this.frequencyDirectory == null) {
      return DEFAULT_FREQUENCYDIRECTORY;
    } else {
      return this.frequencyDirectory;
    }
  }
  
  private String getFunctionDirectory() {
    if (this.functionDirectory == null) {
      return DEFAULT_FUNCTIONDIRECTORY;
    } else {
      return this.functionDirectory;
    }
  }
  
  private String getDiplotypeDirectory() {
    if (this.diplotypeDirectory == null) {
      return DEFAULT_DIPLOTYPEDIRECTORY;
    } else {
      return this.diplotypeDirectory;
    }
  }
  
  private String getRecommendationDirectory() {
    if (this.recommendationDirectory == null) {
      return DEFAULT_RECOMMENDATIONDIRECTORY;
    } else {
      return this.recommendationDirectory;
    }
  }
  
  private String getGeneMappingDirectory() {
    if (this.geneMappingDirectory == null) {
      return DEFAULT_GENEMAPPINGDIRECTORY;
    } else {
      return this.geneMappingDirectory;
    }
  }
  
  private String getTestAlertsDirectory() {
    if (this.testAlertsDirectory == null) {
      return DEFAULT_TESTALERTDIRECTORY;
    } else {
      return this.testAlertsDirectory;
    }
  }
  
  private String getGuidelinesDirectory() {
    if (this.guidelinesDirectory == null) {
      return DEFAULT_GUIDELINESDIRECTORY;
    } else {
      return this.guidelinesDirectory;
    }
  }
}
