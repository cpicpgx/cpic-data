package org.cpicpgx.exporter;

import com.google.common.base.Joiner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.db.ConnectionFactory;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClinVarSubmission {
  private static final String RESULT_FILENAME         = "CPIC.ClinVar.Submission.%s.xlsx";

  private final Connection f_conn;
  private final List<Map<String,String>> f_haplotypeData = new ArrayList<>();
  private final List<Map<String,String>> f_diplotypeData = new ArrayList<>();

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("f", true, "file to write to (.xlsx file)");
    try
        (Connection conn = ConnectionFactory.newConnection())
    {
      CommandLine cli = new DefaultParser().parse(options, args);
      String pathString = cli.getOptionValue("f");
      Path outputPath;
      if (StringUtils.isBlank(pathString)) {
        outputPath = Paths.get(String.format(RESULT_FILENAME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
      } else {
        outputPath = Paths.get(pathString);
      }

      ClinVarSubmission submission = new ClinVarSubmission(conn);
      submission.write(outputPath);

    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  public ClinVarSubmission(Connection conn) throws Exception {
    f_conn = conn;
    try (
        PreparedStatement pstmt = f_conn.prepareStatement("select distinct jsonb_object_keys(lookupkey) gene from recommendation order by 1");
        ResultSet rs = pstmt.executeQuery()
    ) {
      while (rs.next()) {
        String gene = rs.getString(1);
        List<Map<String,String>> haplotypes = queryHaplotypeData(gene);
        List<Map<String,String>> diplotypes = queryDiplotypeData(gene);

        if (diplotypes.size() > 0) {
          f_haplotypeData.addAll(haplotypes);
          f_diplotypeData.addAll(diplotypes);
          System.out.println("Found " + haplotypes.size() + " haplotypes, " + diplotypes.size() + " diplotypes for " + gene);
        } else {
          System.out.println("Skipping " + gene + " due to no diplotypes");
        }
      }
    }
  }

  List<Map<String,String>> queryHaplotypeData(String gene) throws SQLException {
    return executeQuery(
        "select id::text, genesymbol, name, clinicalfunctionalstatus from allele where genesymbol = ? and clinicalfunctionalstatus is not null",
        gene);
  }

  List<Map<String,String>> queryDiplotypeData(String gene) throws SQLException {
    return executeQuery(
        "select d.genesymbol, d.diplotype, d.function1, d.function2, d.generesult, d.description, r.name as drugname, array_agg('PMID:'||u.pmid) as pmids " +
            "from pair p join publication u on (p.guidelineid=u.guidelineid) join diplotype d on p.genesymbol = d.genesymbol join drug r on r.drugid=p.drugid " +
            "where p.removed is false and p.usedforrecommendation is true and p.genesymbol=? " +
            "group by d.genesymbol, d.diplotype, d.function1, d.function2, d.generesult, d.description, r.name order by d.genesymbol, d.diplotype, r.name",
        gene);
  }

  List<Map<String,String>> executeQuery(@Language("PostgreSQL") String query, String gene) throws SQLException {
    List<Map<String,String>> results = new ArrayList<>();
    try (
        PreparedStatement pstmt = f_conn.prepareStatement(query)
    ) {
      pstmt.setString(1, gene);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          ResultSetMetaData meta = rs.getMetaData();
          Map<String,String> resultData = new HashMap<>();
          for (int i=1; i<=meta.getColumnCount(); i++) {
            if (meta.getColumnType(i) == Types.ARRAY) {
              Array array = rs.getArray(i);
              resultData.put(meta.getColumnName(i), Joiner.on(";").join((String[])array.getArray()));
            } else {
              resultData.put(meta.getColumnName(i), rs.getString(i));
            }
          }
          results.add(resultData);
        }
      }
    }
    return results;
  }

  void write(Path outputPath) throws IOException {
    ClinVarWorkbook workbook = new ClinVarWorkbook();

    try (
        PreparedStatement hapStmt = f_conn.prepareStatement("select s.scv from clinvar.submission s " +
            "left join clinvar.orgtrack o on (s.variationid=o.variationid) " +
            "where s.submitter='Clinical Pharmacogenetics Implementation Consortium' " +
            "and o.name=?");
        PreparedStatement dipStmt = f_conn.prepareStatement("select s.scv from clinvar.submission s " +
            "left join clinvar.orgtrack o on (s.variationid=o.variationid) " +
            "where s.submitter='Clinical Pharmacogenetics Implementation Consortium' " +
            "and o.name=? and lower(s.submittedphenotypeinfo)=lower(?)")
    ) {

      for (Map<String, String> hapData : f_haplotypeData) {
        String gene = hapData.get("genesymbol");
        String hapName = hapData.get("name");
        hapStmt.setString(1, gene+hapName);
        try (ResultSet rs = hapStmt.executeQuery()) {
          String scv = "";
          if (rs.next()) {
            scv = rs.getString(1);
          }
          workbook.writeAllele(hapData.get("id"), gene, hapName, hapData.get("clinicalfunctionalstatus"), scv);
        }
      }
      for (Map<String, String> dipData : f_diplotypeData) {
        String diplotype = dipData.get("genesymbol") + dipData.get("diplotype");
        String conditionName = dipData.get("drugname") + " response";
        dipStmt.setString(1, diplotype);
        dipStmt.setString(2, conditionName);
        try (ResultSet rs = dipStmt.executeQuery()) {
          String scv = "";
          if (rs.next()) {
            scv = rs.getString(1);
          }
          workbook.writeDiplotype(dipData.get("genesymbol"), dipData.get("diplotype"), dipData.get("drugname"), dipData.get("generesult"), dipData.get("pmids"), scv);
        }
      }
      workbook.writeTo(outputPath);
      System.out.println("Wrote submission to " + outputPath.toAbsolutePath() + " with " + f_haplotypeData.size() + " haplotypes and " + f_diplotypeData.size() + " diplotypes");
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }
}
