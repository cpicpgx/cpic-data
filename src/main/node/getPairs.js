const axios = require('axios');
const cpicapi = require('./cpicapi');
const dayjs = require('dayjs');
const fs = require('fs');
const _ = require('lodash');

axios.get(
  cpicapi.apiUrl('/pair'),
  {params: {select: '*,drug(name),guideline(url)', order: 'level,genesymbol'}})
.then((r) => {
  const pairs = _.sortBy(r.data.map(p => {
    return {
      chemical: {name: p.drug.name},
      chemicalName: p.drug.name,
      cpicLevel: p.level,
      gene: {name: p.genesymbol},
      guidelineId: p.pairid,
      label: {id: 'x', name: p.pgxtesting},
      pgkbLevel: p.pgkbcalevel,
      pmids: p.citations,
      url: _.get(p, 'guideline.url'),
    }
  }), ['cpicLevel', 'chemicalName']);
  const countByStatus = _.countBy(pairs.map(p => p.cpicLevel));
  const countDrugs = _.uniq(pairs.map(p => _.get(p, 'chemical.name'))).length;
  const countGenes = _.uniq(pairs.map(p => _.get(p, 'gene.name'))).length;
  const lastUpdated = dayjs().format('MMM D, YYYY');
  const payload = {
    countByStatus: countByStatus,
    countDrugs: countDrugs,
    countGenes: countGenes,
    countGuidelines: pairs.length,
    lastUpdated: lastUpdated,
    pairs: pairs,
  };
  
  fs.writeFile(process.argv[2], JSON.stringify(payload, null, 2), (e) => {
    if (e) console.log(e);
    console.log('Done');
  });

  const csv = _.concat(
    [`"Date last updated: ${lastUpdated}"`, 'Gene,Drug,Guideline,CPIC Level,PharmGKB Level of Evidence,PGx on FDA Label,CPIC Publications (PMID)'],
    pairs.map((p) => [p.gene.name, p.chemical.name, p.url, p.cpiclevel, p.pgkbLevel, p.label.name, p.pmids.join(';')].join(',')),
  );
  fs.writeFile(process.argv[3], csv.join('\n'), (e) => {
    if (e) console.log(e);
    console.log('Done CSV');
  });
});