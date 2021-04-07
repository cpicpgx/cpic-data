package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.cpicpgx.util.RowWrapper;
import org.cpicpgx.util.WorkbookWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class ClinVarWorkbook {
  private static final String sf_workbookFileName = "SubmissionTemplate.xlsx";
  private static final String sf_variantSheetName = "Variant";

  private static final String CITATION                = "PMID:21270786";
  private static final String CLIN_SIG_HAPLOTYPE      = "Allele function";
  private static final String ASSERTION_METHOD        = "Clinical Pharmacogenetics Implementation Consortium";
  private static final String CPIC_SITE               = "https://cpicpgx.org";
  private static final String DRUG_RESPONSE           = "drug response";
  private static final String NOT_APPLICABLE          = "not applicable";
  private static final String GERMLINE                = "germline";
  private static final String CURATION                = "curation";

  private static final String sf_colTitleLocalId       = "##Local ID";
  private static final String sf_colTitleGene          = "Gene symbol";
  private static final String sf_colTitleConditionName = "Preferred condition name";
  private static final String sf_colTitleHapName       = "Official allele name";
  private static final String sf_colCitation           = "Assertion method citation";
  private static final String sf_colClinSigCite        = "Clinical significance citations";
  private static final String sf_colAssertionMethod    = "Assertion method";
  private static final String sf_colExplainClinSig     = "Explanation if clinical significance is other or drug response";
  private static final String sf_colUrl                = "Citations or URLs for  clinical significance without database identifiers";
  private static final String sf_colClinSig = "Clinical significance";
  private static final String sf_colCollMethod         = "Collection method";
  private static final String sf_colAlleleOrigin       = "Allele origin";
  private static final String sf_colAffectedStatus     = "Affected status";
  private static final String sf_colClinVarAccession   = "ClinVarAccession";
  private static final String sf_colNovelUpdate        = "Novel or Update";

  private static final int sf_variantTitleRowIdx = 2;
  private final WorkbookWrapper f_workbook;
  private final Sheet f_variantSheet;
  private final Map<String, Integer> f_colHapName = new HashMap<>();
  private int m_dataRowIdx = 5;

  public ClinVarWorkbook() throws IOException {
    try (InputStream is = getClass().getResourceAsStream(sf_workbookFileName)) {
      if (is == null) {
        throw new RuntimeException("Unable to load ClinVar template" + sf_workbookFileName);
      }
      f_workbook = new WorkbookWrapper(is);
      f_workbook.currentSheetIs(sf_variantSheetName);
      f_variantSheet = f_workbook.currentSheet;
    } catch (InvalidParameterException ex) {
      throw new RuntimeException("No title row found", ex);
    }
    RowWrapper titleRow = f_workbook.getRow(sf_variantTitleRowIdx);

    f_colHapName.put(sf_colTitleHapName, null);
    f_colHapName.put(sf_colTitleLocalId, null);
    f_colHapName.put(sf_colTitleGene, null);
    f_colHapName.put(sf_colTitleConditionName, null);
    f_colHapName.put(sf_colCitation, null);
    f_colHapName.put(sf_colAssertionMethod, null);
    f_colHapName.put(sf_colExplainClinSig, null);
    f_colHapName.put(sf_colUrl, null);
    f_colHapName.put(sf_colClinSig, null);
    f_colHapName.put(sf_colClinSigCite, null);
    f_colHapName.put(sf_colCollMethod, null);
    f_colHapName.put(sf_colAlleleOrigin, null);
    f_colHapName.put(sf_colAffectedStatus, null);
    f_colHapName.put(sf_colClinVarAccession, null);
    f_colHapName.put(sf_colNovelUpdate, null);

    StreamSupport.stream(titleRow.row.spliterator(), false)
        .filter(c -> f_colHapName.containsKey(c.getStringCellValue()))
        .forEach(c -> f_colHapName.put(c.getStringCellValue(), c.getColumnIndex()));
  }

  public void writeAllele(String id, String gene, String allele, String function, String scv) {
    Row row = f_variantSheet.createRow(m_dataRowIdx);
    m_dataRowIdx += 1;
    writeCommonFields(row);

    row.createCell(f_colHapName.get(sf_colTitleLocalId), CellType.STRING).setCellValue(id);
    row.createCell(f_colHapName.get(sf_colTitleGene), CellType.STRING).setCellValue(gene);
    row.createCell(f_colHapName.get(sf_colTitleHapName), CellType.STRING).setCellValue(formatAlleleName(gene, allele));
    row.createCell(f_colHapName.get(sf_colTitleConditionName), CellType.STRING).setCellValue(gene + " " + function);
    row.createCell(f_colHapName.get(sf_colExplainClinSig), CellType.STRING).setCellValue(CLIN_SIG_HAPLOTYPE);
    row.createCell(f_colHapName.get(sf_colClinVarAccession), CellType.STRING).setCellValue(scv);
    row.createCell(f_colHapName.get(sf_colNovelUpdate), CellType.STRING).setCellValue(StringUtils.isBlank(scv) ? "Novel" : "Update");
  }

  public void writeDiplotype(String gene, String diplotype, String drug, String phenotype, String pmids, String scv) {
    Row row = f_variantSheet.createRow(m_dataRowIdx);
    m_dataRowIdx += 1;
    writeCommonFields(row);

    row.createCell(f_colHapName.get(sf_colTitleLocalId), CellType.STRING).setCellValue(gene + "|" + diplotype + "|" + drug);
    row.createCell(f_colHapName.get(sf_colTitleGene), CellType.STRING).setCellValue(gene);
    row.createCell(f_colHapName.get(sf_colTitleHapName), CellType.STRING).setCellValue(gene + " " + diplotype);
    row.createCell(f_colHapName.get(sf_colTitleConditionName), CellType.STRING).setCellValue(drug + " response");
    row.createCell(f_colHapName.get(sf_colClinSigCite), CellType.STRING).setCellValue(pmids);
    row.createCell(f_colHapName.get(sf_colExplainClinSig), CellType.STRING).setCellValue(phenotype);
    row.createCell(f_colHapName.get(sf_colClinVarAccession), CellType.STRING).setCellValue(scv);
    row.createCell(f_colHapName.get(sf_colNovelUpdate), CellType.STRING).setCellValue(StringUtils.isBlank(scv) ? "Novel" : "Update");
  }

  private void writeCommonFields(Row row) {
    row.createCell(f_colHapName.get(sf_colAssertionMethod), CellType.STRING).setCellValue(ASSERTION_METHOD);
    row.createCell(f_colHapName.get(sf_colCitation), CellType.STRING).setCellValue(CITATION);
    row.createCell(f_colHapName.get(sf_colUrl), CellType.STRING).setCellValue(CPIC_SITE);
    row.createCell(f_colHapName.get(sf_colClinSig), CellType.STRING).setCellValue(DRUG_RESPONSE);
    row.createCell(f_colHapName.get(sf_colCollMethod), CellType.STRING).setCellValue(CURATION);
    row.createCell(f_colHapName.get(sf_colAlleleOrigin), CellType.STRING).setCellValue(GERMLINE);
    row.createCell(f_colHapName.get(sf_colAffectedStatus), CellType.STRING).setCellValue(NOT_APPLICABLE);
  }

  public void writeTo(Path outputPath) throws IOException {
    try (OutputStream out = Files.newOutputStream(outputPath)) {
      f_workbook.write(out);
    }
  }

  private String formatAlleleName(String gene, String name) {
    if (name.startsWith("*")) {
      return gene + name;
    } else {
      return gene + " " + name;
    }
  }
}
