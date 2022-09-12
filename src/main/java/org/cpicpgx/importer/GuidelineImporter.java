package org.cpicpgx.importer;

import org.apache.commons.lang3.StringUtils;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.DbHarness;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;
import org.cpicpgx.workbook.GuidelineWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.cpicpgx.workbook.GuidelineWorkbook.FILE_NAME;

/**
 * Class to import and update guideline information in the CPIC DB. This will effectively use the CPIC website URL as
 * the unique identifier for the guideline so make sure you have that before adding a new entry to the sheet.
 */
public class GuidelineImporter extends BaseDirectoryImporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    rebuild(new GuidelineImporter(), args);
  }


  @Override
  String getFileExtensionToProcess() {
    return FILE_NAME;
  }

  @Override
  FileType getFileType() {
    return FileType.GUIDELINE;
  }

  @Override
  String[] getDeleteStatements() {
    return new String[0];
  }

  @Override
  void processWorkbook(WorkbookWrapper workbook) throws Exception {
    workbook.switchToSheet(0);
    try (GuidelineDbHarness db = new GuidelineDbHarness()) {
      for (int i = 1; i <= workbook.currentSheet.getLastRowNum(); i++) {
        RowWrapper row = workbook.getRow(i);
        String url = row.getText(GuidelineWorkbook.IDX_URL);

        db.write(
            row.getText(GuidelineWorkbook.IDX_NAME),
            url,
            parseArray(row.getText(GuidelineWorkbook.IDX_ANNOTATIONIDS)),
            parseArray(row.getText(GuidelineWorkbook.IDX_GENES)),
            row.getNullableText(GuidelineWorkbook.IDX_NOTES)
        );

        db.updateDrugs(url, parseArray(row.getText(GuidelineWorkbook.IDX_DRUGS)));
        db.updateLits(url, parseArray(row.getText(GuidelineWorkbook.IDX_PMIDS)));
      }
      db.cleanup();
    }
  }

  private static String[] parseArray(String joinedValues) {
    if (StringUtils.isBlank(joinedValues)) {
      return new String[]{};
    }
    else {
      String[] rawArray = joinedValues.split(";");
      String[] finalArray = new String[rawArray.length];
      for (int i = 0; i < rawArray.length; i++) {
        finalArray[i] = StringUtils.trim(rawArray[i]);
      }
      return finalArray;
    }
  }

  private static class GuidelineDbHarness extends DbHarness {
    final PreparedStatement upsert;
    final PreparedStatement updateDrug;
    final PreparedStatement updateLit;
    final Map<String,String> existingGuidelineMap = new HashMap<>();

    public GuidelineDbHarness() throws SQLException {
      super(FileType.GUIDELINE);
      upsert = prepare("insert into guideline(name, url, pharmgkbid, genes, notesonusage) values (?, ?, ?, ?, ?) on conflict (url) do update set name=excluded.name, pharmgkbid=excluded.pharmgkbid, genes=excluded.genes, notesonusage=excluded.notesonusage");
      updateDrug = prepare("update drug set guidelineid=(select id from guideline where url=?) where name=?");
      updateLit = prepare("update publication set guidelineid=(select id from guideline where url=?) where pmid=?");

      try (
          PreparedStatement existingGuidelines = prepare("select url, name from guideline");
          ResultSet rs = existingGuidelines.executeQuery()
      ) {
        while (rs.next()) {
          existingGuidelineMap.put(rs.getString(1), rs.getString(2));
        }
      }
    }

    void write(String name, String url, String[] pharmgkbIds, String[] genes, String notesOnUsage) throws SQLException {
      upsert.setString(1, name);
      upsert.setString(2, url);
      setNullableArray(upsert,3, pharmgkbIds);
      setNullableArray(upsert,4, genes);
      setNullableString(upsert, 5, notesOnUsage);
      upsert.executeUpdate();

      existingGuidelineMap.remove(url);
    }

    void updateDrug(String url, String drugName) throws SQLException {
      updateDrug.setString(1, url);
      updateDrug.setString(2, drugName);
      updateDrug.executeUpdate();
    }

    void updateDrugs(String url, String[] drugNames) throws SQLException {
      for (String drugName : drugNames) {
        updateDrug(url, drugName);
      }
    }

    void updateLit(String url, String pmid) throws SQLException {
      updateLit.setString(1, url);
      updateLit.setString(2, pmid);
      updateLit.executeUpdate();
    }

    void updateLits(String url, String[] pmids) throws SQLException {
      for (String pmid : pmids) {
        updateLit(url, pmid);
      }
    }

    void cleanup() {
      if (existingGuidelineMap.size() > 0) {
        for (String key : existingGuidelineMap.keySet()) {
          sf_logger.warn("Existing guideline [{}] not covered in imported file, missing data?", existingGuidelineMap.get(key));
        }
      }
    }
  }
}
