package org.cpicpgx.exporter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.FileStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
      System.exit(1);
    }
    System.exit(0);
  }

  private void parseArgs(String[] args) throws ParseException {
    Options options = new Options()
        .addOption("o", true, "output directory, required")
        .addOption("g", true, "gene symbol, required")
        .addOption("d", true,"drug name, optional");
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
    if (!f_genes.isEmpty()) {
      sf_logger.info("Make gene starter for {}", String.join("; ", f_genes));
    }
    if (!f_drugs.isEmpty()) {
      sf_logger.info("Make drug starter for {}", String.join("; ", f_drugs));
    }

    Map<String, Map<FileType, String>> fileMap = new TreeMap<>();
    try (
        QueryHandler queryHandler = new QueryHandler();
        UploadHandler uploadHandler = new UploadHandler(m_path)
    ) {
      Map<String, LookupMethod> geneLookupMap = new LinkedHashMap<>();

      GeneResourceCreator geneResourceCreator = new GeneResourceCreator(m_path);
      for (String gene : f_genes) {
        Map<FileType, String> urlMap = new TreeMap<>();

        LookupMethod lookupMethod = queryHandler.lookupGeneMethod(gene).orElse(LookupMethod.PHENOTYPE);
        geneLookupMap.put(gene, lookupMethod);

        queryHandler.lookupFile(gene, FileType.ALLELE_DEFINITION).ifPresentOrElse(
            (url) -> urlMap.put(FileType.ALLELE_DEFINITION, url),
            () -> {
              AlleleDefinitionWorkbook alleleDefinitionWorkbook = new AlleleDefinitionWorkbook(gene, "NC_#######", "NP_#######", "NG_#######", "NM_#######", 0L);
              alleleDefinitionWorkbook.writeAllele("ALLELE NAME HERE");
              alleleDefinitionWorkbook.writeVariant("VARIANT HERE", "X###X", "g.#####", "g.#####", "rs#####", 1L);
              urlMap.put(FileType.ALLELE_DEFINITION, uploadHandler.upload(alleleDefinitionWorkbook));
            });

        queryHandler.lookupFile(gene, FileType.ALLELE_FUNCTION_REFERENCE).ifPresentOrElse(
            (url) -> urlMap.put(FileType.ALLELE_FUNCTION_REFERENCE, url),
            () -> {
              AlleleFunctionalityReferenceWorkbook alleleFunctionalityReferenceWorkbook = new AlleleFunctionalityReferenceWorkbook(gene);
              urlMap.put(FileType.ALLELE_FUNCTION_REFERENCE, uploadHandler.upload(alleleFunctionalityReferenceWorkbook));
            });

        queryHandler.lookupFile(gene, FileType.FREQUENCY).ifPresentOrElse(
            (url) -> urlMap.put(FileType.FREQUENCY, url),
            () -> {
              FrequencyWorkbook frequencyWorkbook = new FrequencyWorkbook(gene, lookupMethod);
              frequencyWorkbook.writeReferenceHeader(ImmutableSet.of("ALLELE"));
              frequencyWorkbook.writeEthnicityHeader("Example Group", 0);
              urlMap.put(FileType.FREQUENCY, uploadHandler.upload(frequencyWorkbook));
            });

        queryHandler.lookupFile(gene, FileType.GENE_CDS).ifPresentOrElse(
            (url) -> urlMap.put(FileType.GENE_CDS, url),
            () -> {
              GeneCdsWorkbook geneCdsWorkbook = new GeneCdsWorkbook(gene, lookupMethod);
              urlMap.put(FileType.GENE_CDS, uploadHandler.upload(geneCdsWorkbook));
            });

        queryHandler.lookupFile(gene, FileType.GENE_PHENOTYPE).ifPresentOrElse(
            (url) -> urlMap.put(FileType.GENE_PHENOTYPE, url),
            () -> {
              PhenotypesWorkbook phenotypesWorkbook = new PhenotypesWorkbook(gene);
              urlMap.put(FileType.GENE_PHENOTYPE, uploadHandler.upload(phenotypesWorkbook));
            });

        queryHandler.lookupFile(gene, FileType.GENE_RESOURCE).ifPresentOrElse(
            (url) -> urlMap.put(FileType.GENE_RESOURCE, url),
            () -> urlMap.put(FileType.GENE_RESOURCE, uploadHandler.upload(geneResourceCreator.create(gene)))
        );

        fileMap.put(gene, urlMap);
      }
      if (geneResourceCreator.getNoGeneFoundSet().size() > 0) {
        throw new RuntimeException("No genes found in PharmGKB for: " + String.join("; ", geneResourceCreator.getNoGeneFoundSet()));
      }

      DrugResourceCreator drugResourceCreator = new DrugResourceCreator(m_path.toString());
      for (String drug : f_drugs) {
        Map<FileType, String> urlMap = new TreeMap<>();
        String drugId = queryHandler.lookupDrugId(drug).orElse(null);

        queryHandler.lookupFile(drugId, FileType.DRUG_RESOURCE).ifPresentOrElse(
            (url) -> urlMap.put(FileType.DRUG_RESOURCE, url),
            () -> urlMap.put(FileType.DRUG_RESOURCE, uploadHandler.upload(drugResourceCreator.create(drug)))
        );

        queryHandler.lookupFile(drugId, FileType.RECOMMENDATION).ifPresentOrElse(
            (url) -> urlMap.put(FileType.RECOMMENDATION, url),
            () -> {
              RecommendationWorkbook recommendationWorkbook = new RecommendationWorkbook(drug, geneLookupMap);
              recommendationWorkbook.setupSheet("population general");
              queryHandler.writePhenotypeCombos(recommendationWorkbook, geneLookupMap);
              urlMap.put(FileType.RECOMMENDATION, uploadHandler.upload(recommendationWorkbook));
            });

        queryHandler.lookupFile(drugId, FileType.TEST_ALERT).ifPresentOrElse(
            (url) -> urlMap.put(FileType.TEST_ALERT, url),
            () -> {
              TestAlertWorkbook testAlertWorkbook = new TestAlertWorkbook(drug);
              testAlertWorkbook.writeSheet("population general", geneLookupMap);
              queryHandler.writeAlertCombos(testAlertWorkbook, geneLookupMap, drug);
              urlMap.put(FileType.TEST_ALERT, uploadHandler.upload(testAlertWorkbook));
            });

        fileMap.put(drug, urlMap);
      }
    }

    StringBuilder text = new StringBuilder();
    for (String obj : fileMap.keySet()) {
      text.append("## ").append(obj).append("\n");
      Map<FileType, String> urlMap = fileMap.get(obj);
      for (FileType type : urlMap.keySet()) {
        text.append("- ").append(type.name()).append(" => ").append(urlMap.get(type)).append("\n");
      }
      text.append("\n");
    }
    sf_logger.info("Files to use for this guideline\n" + text.toString());
  }

  private static class UploadHandler implements AutoCloseable {
    private final FileStoreClient f_fileStoreClient;
    private final String f_timestamp;
    private final Path f_localPath;

    UploadHandler(Path localPath) {
      f_fileStoreClient = new FileStoreClient();
      f_timestamp = String.valueOf(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
      f_localPath = localPath;
    }

    String upload(AbstractWorkbook workbook) {
      workbook.writeChangeLog(ImmutableList.of(new Object[]{new Date(), "File created"}));
      workbook.getSheets().forEach(SheetWrapper::autosizeColumns);
      Path filePath = f_localPath.resolve(workbook.getFilename());
      try (OutputStream out = Files.newOutputStream(filePath)) {
        workbook.write(out);
      } catch (IOException e) {
        throw new RuntimeException("Error writing file: " + filePath, e);
      }
      return f_fileStoreClient.putGuidelineStagingFile(filePath, f_timestamp);
    }

    String upload(Path filePath) {
      return f_fileStoreClient.putGuidelineStagingFile(filePath, f_timestamp);
    }

    @Override
    public void close() throws Exception {
      f_fileStoreClient.close();
    }
  }


  private static class QueryHandler implements AutoCloseable {
    final Connection f_conn;
    final PreparedStatement f_geneStmt;
    final PreparedStatement f_drugStmt;
    final PreparedStatement f_fileStmt;

    QueryHandler() throws SQLException {
      f_conn = ConnectionFactory.newConnection();
      f_geneStmt = f_conn.prepareStatement("select g.lookupmethod from gene g where g.symbol=?");
      f_drugStmt = f_conn.prepareStatement("select d.drugid from drug d where d.name=?");
      f_fileStmt = f_conn.prepareStatement("select url from file_artifact where ? && entityids and type=?");
    }

    Optional<String> lookupDrugId(String name) throws SQLException {
      String id = null;
      f_drugStmt.setString(1, name);
      try (ResultSet rs = f_drugStmt.executeQuery()) {
        if (rs.next()) {
          id = rs.getString(1);
        } else if (rs.next()) {
          throw new RuntimeException("More than one ID found for " + name);
        }
      }
      return Optional.ofNullable(id);
    }

    Optional<LookupMethod> lookupGeneMethod(String symbol) throws SQLException {
      String lookupMethod = null;
      f_geneStmt.setString(1, symbol);
      try (ResultSet rs = f_geneStmt.executeQuery()) {
        if (rs.next()) {
          lookupMethod = rs.getString(1);
        } else if (rs.next()) {
          throw new RuntimeException("More than one record found for " + symbol);
        }
      }
      LookupMethod lookupMethodEnum = LookupMethod.valueOf(lookupMethod);

      return Optional.of(lookupMethodEnum);
    }

    Optional<String> lookupFile(String entityId, FileType fileType) throws SQLException, URISyntaxException, MalformedURLException {
      if (StringUtils.isBlank(entityId)) {
        return Optional.empty();
      }

      String url = null;
      f_fileStmt.setObject(1, f_conn.createArrayOf("text", new String[]{entityId}));
      f_fileStmt.setString(2, fileType.name());
      try (ResultSet rs = f_fileStmt.executeQuery()) {
        if (rs.next()) {
          String rawUrl = rs.getString(1);
          URL parsedUrl = new URL(rawUrl);
          URI parsedUri = new URI(parsedUrl.getProtocol(), null, parsedUrl.getHost(), parsedUrl.getPort(), parsedUrl.getPath(), parsedUrl.getQuery(), parsedUrl.getRef());
          url = parsedUri.toURL().toString();
        } else if (rs.next()) {
          throw new RuntimeException("More than one record found for " + entityId + " " + fileType);
        }
      }
      return Optional.ofNullable(url);
    }

    private void writeAlertCombos(TestAlertWorkbook workbook, Map<String, LookupMethod> geneMap, String drug) {
      Map<String,String> aliases = new TreeMap<>();
      int i=1;
      for (String gene : geneMap.keySet()) {
        aliases.put("g" + i, gene);
        i += 1;
      }
      String selectClause = aliases.keySet().stream().map(a -> a + ".result, " + a + ".activityscore").collect(Collectors.joining(", "));
      String fromClause = aliases.keySet().stream().map(a -> "gene_result " + a).collect(Collectors.joining(" cross join "));
      String whereClause = aliases.keySet().stream().map(a -> a + ".genesymbol='"+aliases.get(a)+"' ").collect(Collectors.joining(" and "));

      String query = String.format("select distinct %s from %s where %s", selectClause, fromClause, whereClause);

      Map<String, String> phenoMap = new TreeMap<>();
      Map<String, String> scoreMap = new TreeMap<>();
      Map<String, String> alleleMap = new TreeMap<>();
      try (ResultSet rs = f_conn.prepareStatement(query).executeQuery()) {
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
      } catch (SQLException ex) {
        throw new RuntimeException("DB Error", ex);
      }
    }

    private void writePhenotypeCombos(RecommendationWorkbook workbook, Map<String, LookupMethod> geneMap) {
      Map<String,String> aliases = new TreeMap<>();
      int i=1;
      for (String gene : geneMap.keySet()) {
        aliases.put("g" + i, gene);
        i += 1;
      }
      String selectClause = aliases.keySet().stream().map(a -> a + ".result, " + a + ".activityscore").collect(Collectors.joining(", "));
      String fromClause = aliases.keySet().stream().map(a -> "gene_result " + a).collect(Collectors.joining(" cross join "));
      String whereClause = aliases.keySet().stream().map(a -> a + ".genesymbol='"+aliases.get(a)+"' ").collect(Collectors.joining(" and "));

      String query = String.format("select distinct %s from %s where %s", selectClause, fromClause, whereClause);

      Map<String, String> phenoMap = new TreeMap<>();
      Map<String, String> scoreMap = new TreeMap<>();
      Map<String, String> alleleMap = new TreeMap<>();
      Map<String, String> implMap = new TreeMap<>();
      try (ResultSet rs = f_conn.prepareStatement(query).executeQuery()) {
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
          workbook.writeRec(phenoMap, scoreMap, implMap, alleleMap, "", "", "");
        }
      } catch (SQLException ex) {
        throw new RuntimeException("DB Error", ex);
      }
    }

    @Override
    public void close() throws Exception {
      f_conn.close();
    }
  }
}
