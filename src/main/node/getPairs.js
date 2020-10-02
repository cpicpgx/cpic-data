const axios = require('axios');
const cpicapi = require('./cpicapi');
const dayjs = require('dayjs');
const fs = require('fs');
const Papa = require('papaparse');
const _ = require('lodash');

const defaultFilename = 'cpicPairs';

exports.getPairs = (path) => {
  axios.get(
    cpicapi.apiUrl('/pair_view'),
    {params: {order: 'cpiclevel,drugname'}})
    .then((r) => {

      // Compile the data from the API response
      const pairs = r.data;
      const countDrugs = _.uniq(pairs.map(p => _.get(p, 'drugname'))).length;
      const countGenes = _.uniq(pairs.map(p => _.get(p, 'genesymbol'))).length;
      const lastUpdated = dayjs().format('MMM D, YYYY');
      const payload = {
        countDrugs,
        countGenes,
        countGuidelines: pairs.length,
        lastUpdated,
        pairs: _.map(pairs, (p) => {
          const newP = p;
          p.gene = p.genesymbol;
          p.level = p.cpiclevel;
          p.citations = p.pmids;
          return newP;
        }),
      };

      // Make the JSON file
      const jsonFile = `${path}/${defaultFilename}.json`;
      fs.writeFile(jsonFile, JSON.stringify(payload, null, 2), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${jsonFile}`);
      });

      // Make the CSV file
      const fields = [
        'Gene',
        'Drug',
        'Guideline',
        'CPIC Level',
        'CPIC Level Status',
        'PharmGKB Level of Evidence',
        'PGx on FDA Label',
        'CPIC Publications (PMID)'
      ];
      const data = pairs.map((p) => [
        p.genesymbol,
        p.drugname,
        p.guidelineurl,
        p.cpiclevel,
        p.provisional ? 'Provisional' : 'Final',
        p.pgkbcalevel,
        p.pgxtesting,
        _.join(p.pmids, ';')
      ]);
      const csv = Papa.unparse({fields, data});

      const csvFile = `${path}/${defaultFilename}.csv`;
      fs.writeFile(csvFile, `"Date last updated: ${lastUpdated}"\n` + csv, (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${csvFile}`);
      });
    })
    .catch((err) => console.log('Error:', err));
};
