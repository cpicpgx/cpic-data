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
 * the unique identifier for the guideline, so make sure you have that before adding a new entry to the sheet.
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
        String clinpgxId = row.getText(GuidelineWorkbook.IDX_CLINPGXID);

        db.write(
            row.getText(GuidelineWorkbook.IDX_NAME),
            clinpgxId,
            parseArray(row.getText(GuidelineWorkbook.IDX_GENES)),
            row.getNullableText(GuidelineWorkbook.IDX_NOTES)
        );

        db.updateDrugs(clinpgxId, parseArray(row.getText(GuidelineWorkbook.IDX_DRUGS)));

        String pmids = row.getNullableText(GuidelineWorkbook.IDX_PMIDS);
        if (pmids == null) {
            sf_logger.warn("No PMID specified for row {}", i + 1);
        }
        db.updateLits(clinpgxId, parseArray(pmids));
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
      upsert = prepare("insert into guideline(name, clinpgxid, genes, notesonusage) values (?, ?, ?, ?) on conflict (clinpgxid) do update set name=excluded.name, genes=excluded.genes, notesonusage=excluded.notesonusage");
      updateDrug = prepare("update drug set guidelineid=(select id from guideline where clinpgxid=?) where lower(name)=lower(?)");
      updateLit = prepare("update publication set guidelineid=(select id from guideline where clinpgxid=?) where pmid=?");

      try (
          PreparedStatement existingGuidelines = prepare("select clinpgxid, name from guideline");
          ResultSet rs = existingGuidelines.executeQuery()
      ) {
        while (rs.next()) {
          existingGuidelineMap.put(rs.getString(1), rs.getString(2));
        }
      }
    }

    void write(String name, String clinpgxId, String[] genes, String notesOnUsage) throws SQLException {
      upsert.setString(1, name);
      upsert.setString(2, clinpgxId);
      setNullableArray(upsert,3, genes);
      setNullableString(upsert, 4, notesOnUsage);
      upsert.executeUpdate();

      existingGuidelineMap.remove(clinpgxId);
    }

    void updateDrug(String clinpgxId, String drugName) throws SQLException {
      updateDrug.setString(1, clinpgxId);
      updateDrug.setString(2, drugName);
      int changed = updateDrug.executeUpdate();

      if (changed == 0) {
        throw new RuntimeException("Could not find drug record to update for: " + drugName);
      }
    }

    void updateDrugs(String clinpgxId, String[] drugNames) throws SQLException {
      for (String drugName : drugNames) {
        updateDrug(clinpgxId, drugName);
      }
    }

    void updateLit(String clinpgxId, String pmid) throws SQLException {
      updateLit.setString(1, clinpgxId);
      updateLit.setString(2, pmid);
      updateLit.executeUpdate();
    }

    void updateLits(String clinpgxId, String[] pmids) throws SQLException {
      for (String pmid : pmids) {
        updateLit(clinpgxId, pmid);
      }
    }

    void cleanup() {
      if (!existingGuidelineMap.isEmpty()) {
        for (String key : existingGuidelineMap.keySet()) {
          sf_logger.warn("Existing guideline [{}] not covered in imported file, missing data?", existingGuidelineMap.get(key));
        }
      }
    }
  }
}
