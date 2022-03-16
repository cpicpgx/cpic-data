package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.workbook.AlleleDefinitionWorkbook;
import org.cpicpgx.model.FileType;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Write allele definitions out to Excel XLSX files, one per gene.
 *
 * @author Ryan Whaley
 */
public class AlleleDefinitionExporter extends BaseExporter {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Main executable, needs a -d argument to specify the directory to write to
   * @param args the CLI arguments
   */
  public static void main(String[] args) {
    AlleleDefinitionExporter exporter = new AlleleDefinitionExporter();
    try {
      exporter.parseArgs(args);
      exporter.export();
    } catch (Exception ex) {
      sf_logger.error("Error exporting allele definitions", ex);
    }
  }

  public FileType getFileType() {
    return FileType.ALLELE_DEFINITION;
  }

  /**
   * Export all genes with allele definitions, 1 file per gene.
   * @throws Exception can occur from IO or database activity
   */
  public void export() throws Exception {
    try (Connection conn = ConnectionFactory.newConnection();
         PreparedStatement geneStmt = conn.prepareStatement(
             "select distinct g.symbol, g.chromosequenceid, g.proteinsequenceid, g.genesequenceid, g.mrnasequenceid, g.notesonallelenaming, " +
                 "sum(case when ad.pharmvarid is null then 0 else 1 end) pvIds, " +
                 "sum(case when ad.structuralvariation is true then 1 else 0 end) as svs " +
                 "from gene g join allele_definition ad on g.symbol = ad.genesymbol " +
                 "where symbol not in ('HLA-A', 'HLA-B') " +
                 "group by g.symbol, g.chromosequenceid, g.proteinsequenceid, g.genesequenceid, g.mrnasequenceid, g.notesonallelenaming " +
                 "order by 1");
         ResultSet grs = geneStmt.executeQuery()
    ) {
      while (grs.next()) {
        String symbol = grs.getString(1);
        String seqChr = grs.getString(2);
        String seqPro = grs.getString(3);
        String seqGen = grs.getString(4);
        String seqMrna = grs.getString(5);
        String namingNote = grs.getString(6);
        Long pvCount = grs.getLong(7);
        boolean hasStructuralVariation = grs.getLong(8) > 0;

        AlleleDefinitionWorkbook workbook = new AlleleDefinitionWorkbook(symbol, seqChr, seqPro, seqGen, seqMrna, pvCount, namingNote);

        try (PreparedStatement seqLocStmt = conn.prepareStatement(
            "select name, proteinlocation, chromosomelocation, genelocation, dbsnpid, id from sequence_location where geneSymbol=?"
        )) {
          seqLocStmt.setString(1, symbol);
          try (ResultSet rs = seqLocStmt.executeQuery()) {
            while (rs.next()) {
              workbook.writeVariant(
                  rs.getString(1), 
                  rs.getString(2), 
                  rs.getString(3),
                  rs.getString(4),
                  rs.getString(5),
                  rs.getLong(6)
              );
            }
          }
        }

        if (hasStructuralVariation) {
          workbook.writeStructuralVariantHeader();
        }
      
        try (
            PreparedStatement alleleStmt = conn.prepareStatement("select name, id, reference, structuralvariation, pharmvarid from allele_definition where geneSymbol=?");
            PreparedStatement locValStmt = conn.prepareStatement("select locationid, variantallele from allele_location_value where alleledefinitionid=?")
        ) {
          alleleStmt.setString(1, symbol);
          try (ResultSet rs = alleleStmt.executeQuery()) {
            
            // parse to an intermediary map so we can sort the allele names properly
            SortedMap<String, Long> alleleMap = new TreeMap<>(HaplotypeNameComparator.getComparator());
            String referenceAllele = null;
            Map<String,String> strucVarIdMap = new HashMap<>();
            while (rs.next()) {
              boolean isReference = rs.getBoolean(3);
              boolean isStrucVar = rs.getBoolean(4);
              if (isReference) {
                referenceAllele = rs.getString(1);
              }
              if (isStrucVar) {
                strucVarIdMap.put(rs.getString(1), rs.getString(5));
              }
              alleleMap.put(rs.getString(1), rs.getLong(2));
            }

            // We do this so the reference allele is always listed first and the rest are in natural order
            List<String> nameOutputOrderList = new ArrayList<>(alleleMap.keySet());
            nameOutputOrderList.remove(referenceAllele);
            nameOutputOrderList.add(0, referenceAllele);

            for (String alleleName : nameOutputOrderList) {
              Long alleleId = alleleMap.get(alleleName);
              workbook.writeAllele(alleleName);
              locValStmt.setLong(1, alleleId);
              try (ResultSet vrs = locValStmt.executeQuery()) {
                while (vrs.next()) {
                  workbook.writeAlleleLocationValue(vrs.getLong(1), vrs.getString(2));
                }
              }
              if (strucVarIdMap.containsKey(alleleName)) {
                workbook.writeStructrualVariantCell(strucVarIdMap.get(alleleName));
              }
            }
          }
        }

        workbook.writeNotes(queryNotes(conn, symbol, FileType.ALLELE_DEFINITION));

        workbook.writeChangeLog(queryChangeLog(conn, symbol, getFileType()));

        writeWorkbook(workbook);
        addFileExportHistory(workbook.getFilename(), new String[]{symbol});
      }
      handleFileUpload();
    }
  }
}
