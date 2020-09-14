const axios = require('axios');
const cpicapi = require('./cpicapi');
const dayjs = require('dayjs');
const fs = require('fs');
const _ = require('lodash');

const defaultFilename = 'cpicPairs';

exports.getPairs = (path) => {
  axios.get(
    cpicapi.apiUrl('/pair_view'),
    {params: {order: 'cpiclevel,drugname'}})
    .then((r) => {
      const pairs = r.data;
      const countDrugs = _.uniq(pairs.map(p => _.get(p, 'drugname'))).length;
      const countGenes = _.uniq(pairs.map(p => _.get(p, 'genesymbol'))).length;
      const lastUpdated = dayjs().format('MMM D, YYYY');
      const payload = {
        countDrugs,
        countGenes,
        countGuidelines: pairs.length,
        lastUpdated,
        pairs: _.map(pairs, (p) => _.set(p, 'gene', p.genesymbol)),
      };

      const jsonFile = `${path}/${defaultFilename}.json`;
      fs.writeFile(jsonFile, JSON.stringify(payload, null, 2), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${jsonFile}`);
      });

      const csvFile = `${path}/${defaultFilename}.csv`;
      const csv = _.concat(
        [`"Date last updated: ${lastUpdated}"`, 'Gene,Drug,Guideline,CPIC Level,PharmGKB Level of Evidence,PGx on FDA Label,CPIC Publications (PMID)'],
        pairs.map((p) => [p.genesymbol, p.drugname, p.guidelineurl, p.cpiclevel, p.pgkbcalevel, p.pgxtesting, _.join(p.pmids, ';')].join(',')),
      );
      fs.writeFile(csvFile, csv.join('\n'), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${csvFile}`);
      });
    })
    .catch((err) => console.log('Error:', err));
};
