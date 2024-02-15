package org.cpicpgx.exporter;

import org.cpicpgx.db.ConnectionFactory;
import org.cpicpgx.model.FileType;
import org.cpicpgx.util.TextUtils;
import org.cpicpgx.workbook.AlleleDefinitionWorkbook;
import org.jetbrains.annotations.NotNull;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.pharmgkb.common.util.ComparatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.SortedSet;
import java.util.TreeSet;

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
            "select name, proteinlocation, chromosomelocation, genelocation, dbsnpid, id from sequence_location where geneSymbol=? order by position"
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
            PreparedStatement alleleStmt = conn.prepareStatement("select name, id, matchesreferencesequence, structuralvariation, pharmvarid from allele_definition where geneSymbol=?");
            PreparedStatement locValStmt = conn.prepareStatement("select locationid, variantallele from allele_location_value where alleledefinitionid=?")
        ) {
          SortedSet<Allele> alleles = new TreeSet<>();
          alleleStmt.setString(1, symbol);
          try (ResultSet rs = alleleStmt.executeQuery()) {
            while (rs.next()) {
              Allele allele = new Allele(
                  TextUtils.normalize(rs.getString(1)),
                  rs.getLong(2),
                  rs.getBoolean(3),
                  rs.getBoolean(4),
                  rs.getString(5)
              );
              alleles.add(allele);
            }
          }

          for (Allele allele : alleles) {
            workbook.writeAllele(allele.name);
            locValStmt.setLong(1, allele.id);
            try (ResultSet vrs = locValStmt.executeQuery()) {
              while (vrs.next()) {
                workbook.writeAlleleLocationValue(vrs.getLong(1), vrs.getString(2));
              }
            }
            if (allele.structuralVariant) {
              workbook.writeStructrualVariantCell(allele.pharmVarId);
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

  /**
   * Class for properly storing and sorting allele information from the DB record.
   */
  private static class Allele implements Comparable<Allele> {
    public final String name;
    public final long id;
    public final boolean reference;
    public final boolean structuralVariant;
    public final String pharmVarId;

    public Allele(String name, long id, boolean reference, boolean structuralVariant, String pharmVarId) {
      this.name = name;
      this.id = id;
      this.reference = reference;
      this.pharmVarId = pharmVarId;
      this.structuralVariant = structuralVariant;
    }


    @Override
    public int compareTo(@NotNull Allele o) {
      if (this == o) {
        return 0;
      }
      int rez = ComparatorUtils.compare(reference, o.reference) * -1;
      if (rez != 0) {
        return rez;
      }
      return HaplotypeNameComparator.getComparator().compare(name, o.name);
    }
  }
}
