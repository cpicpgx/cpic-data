/**
 * This script writes allele definition data to JSON files, one per Gene. These definition files should be used in
 * PharmCAT.
 *
 * IMPORTANT: you may need to set the AWS_PROFILE env var if you're using a different profile for CPIC
 */

const fs = require('fs');
const path = require('path');
const db = require('./db');
const _ = require('lodash');
const S3 = require('aws-sdk/clients/s3');
const { program } = require('commander');
const { promisify } = require('util');
const exec = promisify(require('child_process').exec);

program.version('1.0');
program
  .option('-o, --output-path <path>', 'directory path to write output to')
  .option('-u, --upload', 'should the generated files be uploaded');
program.parse(process.argv);
const options = program.opts();

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

const uploadToS3 = (fileName, content) => {
  if (options.upload) {
    const upload = new S3.ManagedUpload({
      params: {
        Bucket: 'files.cpicpgx.org',
        Key: `data/report/current/${fileName}`,
        Body: content
      }
    });
    upload.send(
      (err, data) => {
        if (err) {
          console.error(err);
        } else {
          console.log(`uploaded ${data.Location}`);
        }
      },
    );
  }
}

/**
 * Query information about the variants used to define a particular gene
 * @param gene the gene symbol to find locations for
 * @returns {Promise<[]>} an array of location data
 */
const lookupVariants = async (gene) => {
  const rez = await db.many(`
      select sl.chromosomelocation, sl.dbsnpid, sl.id, g.chr, sl.position
      from allele_definition a
               join allele_location_value alv on a.id = alv.alleledefinitionid
               join sequence_location sl on alv.locationid = sl.id
               join gene g on a.genesymbol = g.symbol
      where a.genesymbol=$(gene) and a.matchesreferencesequence is true
      order by sl.position
  `, {gene});
  const payload = [];
  for (let i = 0; i < rez.length; i++) {
    const r = rez[i];
    const cpicAlleles = await lookupVariantAlleles(r.id);

    payload.push({
      chromosome: r.chr,
      cpicPosition: r.position,
      rsid: r.dbsnpid,
      chromosomeHgvsName: r.chromosomelocation,
      sequenceLocationId: r.id,
      cpicAlleles,
    });
  }
  return payload;
}

/**
 * Queries the DB for genes that have allele definitions. This limits to genes that have a "matchesreferencesequence" allele in order
 * to avoid non-standard allele sets like the HLA's.
 * @returns {Promise<Object[]>} an array of gene objects
 */
const lookupGenes = async () => {
  return await db.many(`
      select distinct a.genesymbol, g.chr, g.genesequenceid, g.chromosequenceid, g.proteinsequenceid
      from allele_definition a join gene g on a.genesymbol = g.symbol
      where a.matchesreferencesequence is true
      order by 1
    `);
};

/**
 * Queries the DB for genes that have alleles. This is different than the list of genes that have allele definitions
 * because not all alleles used in the system have definitions (e.g. HLAs).
 * @returns {Promise<Object[]>} an array of objects with the genesymbol property
 */
const lookupGenesWithAlleles = async () => {
  return await db.many(`
      select distinct genesymbol from allele where clinicalfunctionalstatus is not null order by 1
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
 * @returns {Promise<*[]>} an array of objects with a "variantallele" property
 */
const lookupVariantAlleles = async (sequenceLocationId) => {
  try {
    const rez = await db.many(`
        select distinct v.variantallele from allele_location_value v
            join allele_definition ad on v.alleledefinitionid = ad.id
        where v.locationid=$(sequenceLocationId) and ad.structuralvariation is false
    `, {sequenceLocationId});
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
    return await db.many(`
        select a.name, a.id::text as id, array_agg(v.variantallele order by sl.position) as "cpicAlleles", a.matchesreferencesequence 
        from allele_definition a join sequence_location sl on a.genesymbol = sl.genesymbol 
            left join allele_location_value v on (a.id=v.alleledefinitionid and sl.id=v.locationid)
        where a.genesymbol=$(gene) and a.structuralvariation is false and sl.id in (select locationid from allele_location_value)
        group by a.matchesreferencesequence, a.name, a.id::text order by a.matchesreferencesequence desc, a.name
        `, {gene});
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

const lookupActivity = async (gene) => {
  try {
    const rez = await db.many('select name, activityvalue from allele where genesymbol=$(gene) and allele.activityvalue is not null order by 1', {gene});
    const payload = {};
    rez.forEach((r) => payload[r.name] = r.activityvalue);
    return payload;
  } catch (err) {
    zeroResultHandler(err, 'Problem querying allele activity');
  }
}

const listDiplotypeData = async (gene) => {
  try {
    return await db.many(`
        select
            diplotype, generesult,
            lookupkey ->> genesymbol as lookupkey,
            diplotypekey -> genesymbol as diplotypekey
        from diplotype where genesymbol=$(gene)`, {gene});
  } catch (err) {
    zeroResultHandler(err, 'Problem querying diplotype result map');
  }
}

/**
 * Write PharmCAT allele definition files to the given directory.
 *
 * This will be the list of genes that the allele matcher will attempt to make matches for.
 *
 * @param dirPath directory to write definition files to
 * @param cpicVersion the version of the CPIC DB this data is from
 * @return {Promise<void>}
 */
const writeAlleleDefinitions = async (dirPath, cpicVersion) => {
  const genes = await lookupGenes();
  const idList = ['gene\tallele\tID'];
  const alleleDefinitions = [];
  for (let i = 0; i < genes.length; i++) {
    const gene = genes[i];

    try {
      const variants = await lookupVariants(gene.genesymbol);
      const namedAlleles = await lookupNamedAlleles(gene.genesymbol);
      const geneAlleleDefinition = {
        formatVersion: 2,
        cpicVersion,
        modificationDate: new Date().toISOString(),
        gene: gene.genesymbol,
        chromosome: gene.chr,
        genomeBuild: 'GRCh38.p13',
        refSeqChromosomeId: gene.chromosequenceid,
        refSeqGeneId: gene.genesequenceid,
        refSeqProteinId: gene.proteinsequenceid,
        notes: await lookupNotes(gene.genesymbol),
        variants,
        namedAlleles,
      };
      alleleDefinitions.push(geneAlleleDefinition);

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
  const alleleDefPath = path.join(dirPath, 'allele_definitions.json');
  await fs.writeFileSync(
    alleleDefPath,
    JSON.stringify(alleleDefinitions, null, 2),
    fileErrorHandler
  );
  console.log(`wrote ${alleleDefPath}`);
  uploadToS3('allele_definitions.json', JSON.stringify(alleleDefinitions, null, 2));

  const idFilePath = path.join(dirPath, 'haplotype_id_list.tsv');
  await fs.writeFileSync(
    idFilePath,
    idList.join('\n'),
    fileErrorHandler
  );
  console.log(`wrote ${idFilePath}`);
  uploadToS3('haplotype_id_list.tsv', idList.join('\n'));
}

/**
 * This will write the list of alleles and diplotypes used by the phenotyper and the reporter. Some genes may have
 * allele information here that are not in allele definition data.
 *
 * @param dirPath - directory path to write to
 * @param cpicVersion - the current version of the CPIC DB
 * @return {Promise<void>}
 */
const writeGenePhenotypes = async (dirPath, cpicVersion) => {
  const filePath = path.join(dirPath, 'gene_phenotypes.json');
  const genes = await lookupGenesWithAlleles();
  const payload = [];
  for (let i = 0; i < genes.length; i++) {
    const gene = genes[i];
    const haplotypes = await lookupAlleleFunctions(gene.genesymbol);
    const activityValues = await lookupActivity(gene.genesymbol);

    if (_.size(haplotypes) > 0) {
      payload.push({
        gene: gene.genesymbol,
        cpicVersion,
        haplotypes,
        activityValues,
        diplotypes: await listDiplotypeData(gene.genesymbol),
      });
    }
  }

  await fs.writeFile(
    filePath,
    JSON.stringify(payload, null, 2),
    fileErrorHandler
  );
  uploadToS3('gene_phenotypes.json', JSON.stringify(payload, null, 2));
  console.log(`wrote ${filePath}`);
}

const lookupDrugs = async () => {
  try {
    return await db.many(`
        with x as (
            select r.drugid, array_agg(distinct gene) as genes from recommendation r, jsonb_object_keys(r.lookupkey) gene group by r.drugid
        )
        select pair.drugid, d.name as drugname, g.name as guidelinename, g.url,
               g.pharmgkbid as guidelinePharmgkbIds,
               coalesce(x.genes, g.genes) genes,
               json_agg(distinct p) as citations,
               g.notesonusage
        from
            guideline g
                join pair on g.id=pair.guidelineid
                join drug d on pair.drugid = d.drugid
                left join publication p on g.id = p.guidelineid
                left join x on pair.drugid = x.drugid
        where pair.usedforrecommendation is true and pair.removed is false
        group by pair.drugid,d.name,g.name,g.url,g.pharmgkbid,coalesce(x.genes, g.genes),g.notesonusage
        order by d.name
    `);
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

const writeGuidelines = async (rootPath, cpicVersion) => {
  const drugs = await lookupDrugs();

  const payload = [];
  for (let i = 0; i < drugs.length; i++) {
    const drug = drugs[i];
    drug.cpicVersion = cpicVersion;
    drug.recommendations = await lookupRecommendations(drug.drugid);
    payload.push(drug);
  }

  const filePath = path.join(rootPath, 'drugs.json');
  await fs.writeFile(filePath, JSON.stringify(payload, null, 2), fileErrorHandler);
  console.log(`wrote ${filePath}`);
  uploadToS3('drugs.json', JSON.stringify(payload, null, 2));
}

const getGitUser = async function getGitVersion () {
  const version = await exec('git describe --tags');
  return version.stdout.trim();
};

try {
  if (!fs.existsSync(options.outputPath)) {
    fs.mkdirSync(options.outputPath);
  }

  getGitUser().then((version) => {
    writeAlleleDefinitions(options.outputPath, version).then(() => console.log('done with allele definitions'));
    writeGenePhenotypes(options.outputPath, version).then(() => console.log('done with gene phenotypes'));
    writeGuidelines(options.outputPath, version).then(() => console.log('done with recommendations'));
  });
} catch (err) {
  console.error('Error writing allele definitions', err);
  process.exit(1);
}
