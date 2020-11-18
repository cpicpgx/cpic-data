/**
 * This script writes allele definition data to JSON files, one per Gene. These definition files should be used in
 * PharmCAT.
 */

const fs = require('fs');
const path = require('path');
const db = require('./db');

const fileErrorHandler = (e) => {
  if (e) {
    console.error('Error writing file', e);
  }
}

const zeroResultHandler = (err, message) => {
  if (err.query && err.received === 0) {
    return [];
  } else {
    console.warn(message, err);
    return undefined;
  }
}

/**
 * Query information about the variants used to define a particular gene
 * @param gene the gene symbol to find locations for
 * @returns {Promise<[]>} an array of location data
 */
const lookupVariants = async (gene) => {
  const rez = await db.many('select sl.chromosomelocation, sl.genelocation, sl.proteinlocation, sl.name, sl.dbsnpid, sl.id from allele_definition a join allele_location_value alv on a.id = alv.alleledefinitionid join sequence_location sl on alv.locationid = sl.id where a.genesymbol=$(gene) and reference is true', {gene});
  const payload = [];
  for (let i = 0; i < rez.length; i++) {
    const r = rez[i];
    payload.push({
      chromosomeHgvsName: r.chromosomelocation,
      geneHgvsName: r.genelocation,
      proteinNote: r.proteinlocation,
      resourceNote: r.name,
      rsid: r.dbsnpid,
      sequenceLocationId: r.id,
    });
  }
  return payload;
}

/**
 * Queries the DB for genes that have allele definitions. This limits to genes that have a "reference" allele in order
 * to avoid non-standard allele sets like the HLA's.
 * @returns {Promise<Object[]>} an array of gene objects
 */
const lookupGenes = async () => {
  return await db.many(`
    select distinct a.genesymbol, g.chr, g.genesequenceid, g.chromosequenceid, g.proteinsequenceid 
    from allele_definition a join gene g on a.genesymbol = g.symbol where a.reference is true order by 1
    `);
};

/**
 * Query the notes about the allele definition for a specific gene
 * @param gene the gene symbol to query
 * @returns {Promise<*[]>} an array of objects with a "note" property
 */
const lookupNotes = async (gene) => {
  try {
    const rez = await db.many(`select note
                          from file_note
                          where entityid = $(gene)
                            and type = 'ALLELE_DEFINITION'`, {gene});
    return rez.map((r) => r.note);
  } catch (err) {
    zeroResultHandler(err, 'Problem querying notes');
  }
}

/**
 * Query the alleles used in the definitions for a particular location used in allele definitions
 * @param sequenceLocationId the primary key ID for a sequence location
 * @returns {Promise<*[]>} an array of objects wiht a "variantallele" property
 */
const lookupVariantAlleles = async (sequenceLocationId) => {
  try {
    const rez = await db.many('select distinct variantallele from allele_location_value where locationid=$(sequenceLocationId)', {sequenceLocationId});
    return rez.map((r) => r.variantallele);
  } catch (err) {
    zeroResultHandler(err, 'Problem querying possible alleles');
  }
}

/**
 * Query the named alleles for a given gene. Give the name, ID, and variant allele values for the haplotype
 * @param gene the gene symbol to query for
 * @returns {Promise<any[]>}
 */
const lookupNamedAlleles = async (gene) => {
  try {
    return await db.many('select a.name, a.id::text as id, array_agg(v.variantallele) as alleles from allele_definition a join sequence_location sl on a.genesymbol = sl.genesymbol left join allele_location_value v on (a.id=v.alleledefinitionid and sl.id=v.locationid) where a.genesymbol=$(gene) group by a.name, a.id::text', {gene});
  } catch (err) {
    zeroResultHandler(err, 'Problem querying possible alleles');
  }
}

const lookupAlleleFunctions = async (gene) => {
  try {
    const rez = await db.many('select name, clinicalfunctionalstatus from allele where genesymbol=$(gene) and clinicalfunctionalstatus is not null order by 1', {gene});
    const payload = {};
    rez.forEach((r) => payload[r.name] = r.clinicalfunctionalstatus);
    return payload;
  } catch (err) {
    zeroResultHandler(err, 'Problem querying allele functions', err);
  }
}

const lookupDiplotypes = async (gene) => {
  try {
    const rez = await db.many('select r.result as phenotype, grl.function1, grl.function2 from gene_result r join gene_result_lookup grl on r.id = grl.phenotypeid where genesymbol=$(gene)', {gene});
    const payload = [];
    rez.forEach((r) => payload.push({phenotype: r.phenotype, diplotype: [r.function1, r.function2]}));
    return payload;
  } catch (err) {
    zeroResultHandler(err, 'Problem querying diplotype data', err);
  }
}

/**
 * Write PharmCAT allele definition files to the given directory
 * @param dirPath directory to write definition files to
 * @return {Promise<void>}
 */
const writeAlleleDefinitions = async (dirPath) => {
  const allelesDir = path.join(dirPath, 'alleles');
  if (!fs.existsSync(allelesDir)) {
    fs.mkdirSync(allelesDir);
  }
  const genes = await lookupGenes();
  const idList = ['gene\tallele\tID'];
  for (let i = 0; i < genes.length; i++) {
    const gene = genes[i];
    const filePath = path.join(allelesDir, `${gene.genesymbol}_translation.json`);

    try {
      const variants = await lookupVariants(gene.genesymbol);
      const variantAlleles = await Promise.all(variants.map(async (v) => await lookupVariantAlleles(v.sequenceLocationId)));
      const namedAlleles = await lookupNamedAlleles(gene.genesymbol);
      const geneFileContent = {
        formatVersion: 1,
        modificationDate: new Date().toISOString(),
        gene: gene.genesymbol,
        chromosome: gene.chr,
        genomeBuild: 'b38',
        refSeqChromosomeId: gene.chromosequenceid,
        refSeqGeneId: gene.genesequenceid,
        refSeqProteinId: gene.proteinsequenceid,
        notes: await lookupNotes(gene.genesymbol),
        variants,
        variantAlleles,
        namedAlleles,
      };
      await fs.writeFile(
        filePath,
        JSON.stringify(geneFileContent, null, 2),
        fileErrorHandler,
      );
      console.log(`wrote ${filePath}`);

      namedAlleles.forEach((a) => idList.push(`${gene.genesymbol}\t${a.name}\t${a.id}`));
    } catch (err) {
      if (err.query && err.received === 0) {
        console.warn(`No alleles exist for ${gene.genesymbol}`);
      } else {
        console.warn(`No data written for ${gene.genesymbol}`);
        throw err;
      }
    }
  }
  const idFilePath = path.join(dirPath, 'haplotype.id.list.tsv');
  await fs.writeFile(
    idFilePath,
    idList.join('\n'),
    fileErrorHandler
  );
  console.log(`wrote ${idFilePath}`);
}

const writeGenePhenotypes = async (dirPath) => {
  const filePath = path.join(dirPath, 'gene.phenotypes.json');
  const genes = await lookupGenes();
  const payload = [];
  for (let i = 0; i < genes.length; i++) {
    const gene = genes[i];
    payload.push({
      gene: gene.genesymbol,
      haplotypes: await lookupAlleleFunctions(gene.genesymbol),
      diplotypes: await lookupDiplotypes(gene.genesymbol),
    });
  }

  await fs.writeFile(
    filePath,
    JSON.stringify(payload, null, 2),
    fileErrorHandler
  );
  console.log(`wrote ${filePath}`);
}

try {
  writeAlleleDefinitions(process.argv[2]);
  writeGenePhenotypes(process.argv[2]);
} catch (err) {
  console.error('Error writing allele definitions', err);
  process.exit(1);
}
