package org.cpicpgx.exporter;

import com.google.common.collect.ImmutableList;
import org.apache.commons.cli.*;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class will create "starter" files for new guidelines. The starter files have standard file, sheet, and column
 * header text with minimal data filled in.
 *
 * The command line options are "g" for gene HGNC symbols and "d" for drug names. 1 or more of each can be specified.
 */
public class GuidelineStarterPack {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Set<String> f_genes = new TreeSet<>();
  private final Set<String> f_drugs = new TreeSet<>();
  private Path m_path;

  public static void main(String[] args) {
    try {
      GuidelineStarterPack starterPack = new GuidelineStarterPack();
      starterPack.parseArgs(args);
      starterPack.execute();
    } catch (Exception e) {
      sf_logger.error("Error making starter pack", e);
    }
  }

  private void parseArgs(String[] args) throws ParseException {
    Options options = new Options()
        .addOption("o", true, "output directory")
        .addOption("g", true, "gene symbol")
        .addOption("d", true,"drug name");
    CommandLine cli = new DefaultParser()
        .parse(options, args);

    if (!cli.hasOption("o")) {
      throw new ParseException("Must specify an output path");
    } else {
      m_path = Paths.get(cli.getOptionValue("o"));
      if (!m_path.toFile().exists() || !m_path.toFile().isDirectory()) {
        throw new ParseException("Output directory must be an existing directory");
      }
    }

    String[] genes = cli.getOptionValues("g");
    f_genes.addAll(Arrays.asList(genes));

    String[] drugs = cli.getOptionValues("d");
    if (drugs != null) {
      f_drugs.addAll(Arrays.asList(drugs));
    }
  }

  private void execute() throws Exception {
    sf_logger.info("Make starter for {} and {}", String.join("/", f_genes), String.join("/", f_drugs));

    try (
        Connection conn = ConnectionFactory.newConnection();
        PreparedStatement geneStmt = conn.prepareStatement("select g.symbol, g.lookupmethod from gene g where g.symbol=?");
        PreparedStatement drugStmt = conn.prepareStatement("select d.drugid from drug d where d.name=?")
    ) {
      List<AbstractWorkbook> workbooksToWrite = new ArrayList<>();
      Map<String, LookupMethod> geneLookupMap = new LinkedHashMap<>();

      for (String gene : f_genes) {
        geneStmt.setString(1, gene);
        try (ResultSet rs = geneStmt.executeQuery()) {
          if (rs.next()) {
            sf_logger.warn("{} already exists, starter files will not include possibly extant data", gene);
          }
          geneLookupMap.put(rs.getString(1), LookupMethod.valueOf(rs.getString(2)));

          AlleleDefinitionWorkbook alleleDefinitionWorkbook = new AlleleDefinitionWorkbook(gene, "NC_#######", "NP_#######", "NG_#######", "NM_#######", 0L);
          alleleDefinitionWorkbook.writeAllele("ALLELE NAME HERE");
          alleleDefinitionWorkbook.writeVariant("VARIANT HERE", "X###X", "g.#####", "g.#####", "rs#####", 1L);
          workbooksToWrite.add(alleleDefinitionWorkbook);

          AlleleFunctionalityReferenceWorkbook alleleFunctionalityReferenceWorkbook = new AlleleFunctionalityReferenceWorkbook(gene);
          workbooksToWrite.add(alleleFunctionalityReferenceWorkbook);

          FrequencyWorkbook frequencyWorkbook = new FrequencyWorkbook(gene);
          frequencyWorkbook.writeEthnicityHeader("Example Group", 0);
          workbooksToWrite.add(frequencyWorkbook);

          GeneCdsWorkbook geneCdsWorkbook = new GeneCdsWorkbook(gene);
          workbooksToWrite.add(geneCdsWorkbook);

          PhenotypesWorkbook phenotypesWorkbook = new PhenotypesWorkbook(gene);
          workbooksToWrite.add(phenotypesWorkbook);

          GeneResourceWorkbook g = new GeneResourceWorkbook(gene);
          g.writeIds("", "", "", "");
          workbooksToWrite.add(g);
        }
      }
      for (String drug : f_drugs) {
        drugStmt.setString(1, drug);
        try (ResultSet rs = drugStmt.executeQuery()) {
          if (rs.next()) {
            sf_logger.info("{} already exists, starter files will not include possibly extant data", rs.getString(1));
          }
          DrugResourceWorkbook w = new DrugResourceWorkbook(drug);
          w.writeMapping("", "", new String[]{}, "");
          workbooksToWrite.add(w);

          RecommendationWorkbook recommendationWorkbook = new RecommendationWorkbook(drug, f_genes);
          recommendationWorkbook.setupSheet("population general");
          writePhenotypeCombos(conn, recommendationWorkbook, f_genes);
          workbooksToWrite.add(recommendationWorkbook);

          TestAlertWorkbook testAlertWorkbook = new TestAlertWorkbook(drug);
          testAlertWorkbook.writeSheet("population general", geneLookupMap);
          writeAlertCombos(conn, testAlertWorkbook, geneLookupMap, drug);
          workbooksToWrite.add(testAlertWorkbook);
        }
      }

      if (workbooksToWrite.isEmpty()) {
        sf_logger.warn("Nothing to do");
      }

      for (AbstractWorkbook w : workbooksToWrite) {
        w.writeChangeLog(ImmutableList.of(new Object[]{new Date(), "File created"}));
        w.getSheets().forEach(SheetWrapper::autosizeColumns);
        Path filePath = m_path.resolve(w.getFilename());
        try (OutputStream out = Files.newOutputStream(filePath)) {
          w.write(out);
        }
        sf_logger.info("Created starter file {}", filePath.toAbsolutePath());
      }
    }
  }

  private static void writePhenotypeCombos(Connection conn, RecommendationWorkbook workbook, Set<String> genes) throws SQLException {
    Map<String,String> aliases = new TreeMap<>();
    int i=1;
    for (String gene : genes) {
      aliases.put("g" + i, gene);
      i += 1;
    }
    String selectClause = aliases.keySet().stream().map(a -> a + ".phenotype, " + a + ".activityscore").collect(Collectors.joining(", "));
    String fromClause = aliases.keySet().stream().map(a -> "gene_phenotype " + a).collect(Collectors.joining(" cross join "));
    String whereClause = aliases.keySet().stream().map(a -> a + ".genesymbol='"+aliases.get(a)+"' ").collect(Collectors.joining(" and "));

    String query = String.format("select distinct %s from %s where %s", selectClause, fromClause, whereClause);

    Map<String, String> phenoMap = new TreeMap<>();
    Map<String, String> scoreMap = new TreeMap<>();
    Map<String, String> implMap = new TreeMap<>();
    try (ResultSet rs = conn.prepareStatement(query).executeQuery()) {
      while (rs.next()) {
        int colIdx = 1;
        for (String alias : aliases.keySet()) {
          phenoMap.put(aliases.get(alias), rs.getString(colIdx));
          colIdx += 1;
          scoreMap.put(aliases.get(alias), rs.getString(colIdx));
          colIdx += 1;
          implMap.put(aliases.get(alias), "");
        }
        workbook.writeRec(phenoMap, scoreMap, implMap, "", "", "");
      }
    }
  }

  private static void writeAlertCombos(Connection conn, TestAlertWorkbook workbook, Map<String, LookupMethod> geneMap, String drug) throws SQLException {
    Map<String,String> aliases = new TreeMap<>();
    int i=1;
    for (String gene : geneMap.keySet()) {
      aliases.put("g" + i, gene);
      i += 1;
    }
    String selectClause = aliases.keySet().stream().map(a -> a + ".phenotype, " + a + ".activityscore").collect(Collectors.joining(", "));
    String fromClause = aliases.keySet().stream().map(a -> "gene_phenotype " + a).collect(Collectors.joining(" cross join "));
    String whereClause = aliases.keySet().stream().map(a -> a + ".genesymbol='"+aliases.get(a)+"' ").collect(Collectors.joining(" and "));

    String query = String.format("select distinct %s from %s where %s", selectClause, fromClause, whereClause);

    Map<String, String> phenoMap = new TreeMap<>();
    Map<String, String> scoreMap = new TreeMap<>();
    Map<String, String> alleleMap = new TreeMap<>();
    try (ResultSet rs = conn.prepareStatement(query).executeQuery()) {
      while (rs.next()) {
        int colIdx = 1;
        for (String alias : aliases.keySet()) {
          String geneSymbol = aliases.get(alias);
          LookupMethod lookupMethod = geneMap.get(geneSymbol);
          switch(lookupMethod) {
            case PHENOTYPE:
              phenoMap.put(geneSymbol, rs.getString(colIdx));
              break;
            case ALLELE_STATUS:
              alleleMap.put(geneSymbol, rs.getString(colIdx));
              break;
            case ACTIVITY_SCORE:
              scoreMap.put(geneSymbol, rs.getString(colIdx));
              break;
            default:
              throw new RuntimeException("Lookup method not implemented");
          }
          colIdx += 1;
        }
        workbook.writeAlert(geneMap, "", new String[0], drug, scoreMap, phenoMap, alleleMap);
      }
    }
  }
}
