const fs = require('fs');
const axios = require('axios');
const cpicapi = require('./cpicapi');
const _ = require('lodash');

const defaultFilename = 'cpic.alleles';

exports.getAlleles = (path) => {
  const url = cpicapi.apiUrl('/allele_guideline_view');
  console.log(`Fetching data from: ${url}`);
  axios.get(url, {})
    .then((r) => {
      const original = r.data;
      const alleles = _.uniqBy(_.map(original, (d) => {
        return {genesymbol: d.genesymbol, allele_name: d.allele_name};
      }), (d) => d.genesymbol+d.allele_name);
      const payload = _.map(alleles, (a) => {
        return {
          allele: {
            name: a.allele_name,
            gene: {
              symbol: a.genesymbol,
            }
          },
          guidelines: _.map(_.filter(original, a), (b) => {
            return {url: b.guideline_url, title: b.guideline_name}
          }),
        };
      });

      const jsonFile = path + "/" + defaultFilename + ".json";
      fs.writeFile(jsonFile, JSON.stringify(payload, null, 2), (e) => {
        if (e) console.log(e);
        console.log('Done writing ' + jsonFile);
      });

      const csv = ['Gene,Allele,Guideline,URL'];
      _.forEach(original, (o) => {
        csv.push(o.genesymbol + ',' + o.allele_name + ',' + o.guideline_name + ',' + o.guideline_url)
      });
      const csvFile = path + "/" + defaultFilename + ".csv";
      fs.writeFile(csvFile, csv.join('\n'), (e) => {
        if (e) console.log(e);
        console.log('Done writing ' + csvFile);
      });
    });
};
