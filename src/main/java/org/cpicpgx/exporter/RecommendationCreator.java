package org.cpicpgx.exporter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.db.LookupMethod;
import org.cpicpgx.workbook.RecommendationWorkbook;
import org.cpicpgx.workbook.SheetWrapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RecommendationCreator {

  private final Map<String,LookupMethod> f_geneMap = new TreeMap<>();
  private final List<Map<String,String>> f_phenoMapList = new ArrayList<>();
  private final List<Map<String,String>> f_scoreMapList = new ArrayList<>();
  private final String f_drugName;


  public RecommendationCreator(String drugName, Map<String, LookupMethod> geneMap) {
    Preconditions.checkNotNull(geneMap);
    Preconditions.checkArgument(geneMap.size() >= 1 && geneMap.size() <= 2, "Only works on 1 or 2 gene recommendations");
    f_geneMap.putAll(geneMap);
    f_drugName = drugName;
  }

  public void loadData() throws SQLException {
    try (Connection conn = ConnectionFactory.newConnection()) {
      if (f_geneMap.size() == 2) {
        PreparedStatement stmt = conn.prepareStatement("with x as (\n" +
            "    select result, activityscore from gene_result where genesymbol = ?\n" +
            "    union\n" +
            "    select 'No '||symbol||' result' as result, 'n/a' as activityscore from gene where symbol=?\n" +
            "), y as (\n" +
            "    select result,activityscore from gene_result where genesymbol=?\n" +
            "    union\n" +
            "    select 'No '||symbol||' result' as result, 'n/a' as activityscore from gene where symbol=?\n" +
            ")\n" +
            "select x.result, x.activityscore, y.result, y.activityscore from x, y");
        List<String> genes = new ArrayList<>(f_geneMap.keySet());
        stmt.setString(1, genes.get(0));
        stmt.setString(2, genes.get(0));
        stmt.setString(3, genes.get(1));
        stmt.setString(4, genes.get(1));
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            f_phenoMapList.add(ImmutableMap.of(genes.get(0), rs.getString(1), genes.get(1), rs.getString(3)));
            f_scoreMapList.add(ImmutableMap.of(genes.get(0), rs.getString(2), genes.get(1), rs.getString(4)));
          }
        }
      }
      else if (f_geneMap.size() == 1) {
        PreparedStatement stmt = conn.prepareStatement("select result, activityscore from gene_result where genesymbol = ?");
        List<String> genes = new ArrayList<>(f_geneMap.keySet());
        stmt.setString(1, genes.get(0));
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            f_phenoMapList.add(ImmutableMap.of(genes.get(0), rs.getString(1)));
            f_scoreMapList.add(ImmutableMap.of(genes.get(0), rs.getString(2)));
          }
        }
      }
    }
  }

  public RecommendationWorkbook write() {
    RecommendationWorkbook workbook = new RecommendationWorkbook(f_drugName, f_geneMap);
    workbook.setupSheet("general");
    for (int i = 0; i < f_phenoMapList.size(); i++) {
      workbook.writeRec(
          f_phenoMapList.get(i),
          f_scoreMapList.get(i),
          ImmutableMap.of(),
          ImmutableMap.of(),
          "",
          "",
          ""
      );
    }
    workbook.writeChangeLog(ImmutableList.of(new Object[]{new Date(), "File created"}));
    workbook.getSheets().forEach(SheetWrapper::autosizeColumns);
    return workbook;
  }
}
