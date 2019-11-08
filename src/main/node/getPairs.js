const axios = require('axios');
const cpicapi = require('./cpicapi');
const dayjs = require('dayjs');
const fs = require('fs');
const _ = require('lodash');

const defaultFilename = 'cpicPairs';

exports.getPairs = (path) => {
  axios.get(
    cpicapi.apiUrl('/gene_drug_pair'),
    {params: {order: 'level,drugname'}})
    .then((r) => {
      const pairs = r.data;
      const countDrugs = _.uniq(pairs.map(p => _.get(p, 'drugname'))).length;
      const countGenes = _.uniq(pairs.map(p => _.get(p, 'gene'))).length;
      const lastUpdated = dayjs().format('MMM D, YYYY');
      const payload = {
        countDrugs,
        countGenes,
        countGuidelines: pairs.length,
        lastUpdated,
        pairs,
      };

      const jsonFile = `${path}/${defaultFilename}.json`;
      fs.writeFile(jsonFile, JSON.stringify(payload, null, 2), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${jsonFile}`);
      });

      const csvFile = `${path}/${defaultFilename}.csv`;
      const csv = _.concat(
        [`"Date last updated: ${lastUpdated}"`, 'Gene,Drug,Guideline,CPIC Level,PharmGKB Level of Evidence,PGx on FDA Label,CPIC Publications (PMID)'],
        pairs.map((p) => [p.gene, p.drugname, p.guidelineurl, p.level, p.pgkbcalevel, p.pgxtesting, _.join(p.citations, ';')].join(',')),
      );
      fs.writeFile(csvFile, csv.join('\n'), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${csvFile}`);
      });
    });
};
