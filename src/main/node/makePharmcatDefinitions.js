/**
 * This script writes allele definition data to JSON files, one per Gene. These definition files should be used in
 * PharmCAT.
 */

const fs = require('fs');
const path = require('path');
const db = require('./db');
const _ = require('lodash');

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
  const rez = await db.many(`
select sl.chromosomelocation, sl.genelocation, sl.proteinlocation, sl.name, sl.dbsnpid, sl.id, g.chr
from allele_definition a join allele_location_value alv on a.id = alv.alleledefinitionid
    join sequence_location sl on alv.locationid = sl.id join gene g on a.genesymbol = g.symbol
where a.genesymbol=$(gene) and reference is true order by sl.chromosomelocation`, {gene});
  const payload = [];
  for (let i = 0; i < rez.length; i++) {
    const positionPattern = /g\.(\d+)/g;
    const r = rez[i];
    const positionMatch = positionPattern.exec(r.chromosomelocation);
    let type = 'SNP';
    if (r.chromosomelocation.includes('ins')) {
      type = 'INS';
    } else if (r.chromosomelocation.includes('del')) {
      type = 'DEL';
    }
    payload.push({
      chromosome: r.chr,
      position: _.toNumber(_.get(positionMatch, '[1]', null)),
      rsid: r.dbsnpid,
      chromosomeHgvsName: r.chromosomelocation,
      geneHgvsName: r.genelocation,
      proteinNote: r.proteinlocation,
      resourceNote: r.name,
      type,
      referenceRepeat: null,
      sequenceLocationId: r.id,
    });
  }
  return payload;
}

/**
 * Queries the DB for genes that have allele definitions. This limits to genes that have a "reference" allele in order
 * to avoid non-standard allele sets like the HLA's. Also explicitly excludes G6PD since it's not supported yet.
 * @returns {Promise<Object[]>} an array of gene objects
 */
const lookupGenes = async () => {
  return await db.many(`
    select distinct a.genesymbol, g.chr, g.genesequenceid, g.chromosequenceid, g.proteinsequenceid 
    from allele_definition a join gene g on a.genesymbol = g.symbol where a.reference is true and g.symbol!='G6PD' order by 1
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
    return await db.many('select a.name, a.id::text as id, array_agg(v.variantallele order by sl.chromosomelocation) as alleles from allele_definition a join sequence_location sl on a.genesymbol = sl.genesymbol left join allele_location_value v on (a.id=v.alleledefinitionid and sl.id=v.locationid) where a.genesymbol=$(gene) group by a.reference, a.name, a.id::text order by a.reference desc, a.name', {gene});
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
    zeroResultHandler(err, 'Problem querying allele functions');
  }
}

const listDiplotypeData = async (gene) => {
  try {
    return await db.many("select diplotype, generesult, description, lookupkey ->> genesymbol lookupkey from diplotype where genesymbol=$(gene)", {gene});
  } catch (err) {
    zeroResultHandler(err, 'Problem querying diplotype result map');
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
    const haplotypes = await lookupAlleleFunctions(gene.genesymbol);

    if (_.size(haplotypes) > 0) {
      payload.push({
        gene: gene.genesymbol,
        haplotypes,
        diplotypes: await listDiplotypeData(gene.genesymbol),
      });
    }
  }

  await fs.writeFile(
    filePath,
    JSON.stringify(payload, null, 2),
    fileErrorHandler
  );
  console.log(`wrote ${filePath}`);
}

const lookupDrugs = async () => {
  try {
    return await db.many(`select d.drugid, d.name as drugname, g.name as guidelinename, g.url, g.pharmgkbid as guidelinePharmgkbIds, array_agg(distinct gene) genes, json_agg(distinct p) as citations
from guideline g join recommendation r on (g.id=r.guidelineid) join drug d on (r.drugid = d.drugid) join publication p on (g.id = p.guidelineid), jsonb_object_keys(r.lookupkey) gene
group by d.drugid, d.name, g.name, g.url, g.pharmgkbid order by d.name`);
  } catch (err) {
    zeroResultHandler(err, 'No guidelines found');
  }
}

const lookupRecommendations = async (drugId) => {
  try {
    return await db.many(`select r.implications, r.drugrecommendation, r.classification, r.phenotypes, 
        r.activityscore, r.allelestatus, r.lookupkey, r.comments, r.population 
        from recommendation r where r.drugid=$(drugId) order by r.lookupkey`, {drugId});
  } catch (err) {
    zeroResultHandler(err, 'No recommendations for a guideline');
  }
}

const writeGuidelines = async (rootPath) => {
  const drugs = await lookupDrugs();

  const payload = [];
  for (let i = 0; i < drugs.length; i++) {
    const drug = drugs[i];
    drug.recommendations = await lookupRecommendations(drug.drugid);
    payload.push(drug);
  }

  const filePath = path.join(rootPath, 'drugs.json');
  await fs.writeFile(filePath, JSON.stringify(payload, null, 2), fileErrorHandler);
  console.log(`wrote ${filePath}`);
}

try {
  writeAlleleDefinitions(process.argv[2]);
  writeGenePhenotypes(process.argv[2]);
  writeGuidelines(process.argv[2]);
} catch (err) {
  console.error('Error writing allele definitions', err);
  process.exit(1);
}
