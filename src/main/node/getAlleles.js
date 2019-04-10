const fs = require('fs');
const axios = require('axios');
const cpicapi = require('./cpicapi');
const _ = require('lodash');

axios.get(cpicapi.apiUrl('/allele_guideline_view'), {})
  .then((r) => {
    const original = r.data;
    const alleles = _.uniq(_.map(original, (d) => {return {genesymbol: d.genesymbol, allele_name: d.allele_name};}));
    const payload = _.map(alleles, (a) => {
      return {
        allele: {
          name: a.allele_name,
          gene: {
            symbol: a.genesymbol,
          }
        },
        guidelines: _.map(_.filter(original, a), (b) => {return {url: b.guideline_url, title: b.guideline_name}}),
      };
    });
    
    fs.writeFile(process.argv[2], JSON.stringify(payload, null, 2), (e) => {
      if (e) console.log(e);
      console.log('Done');
    });

    const csv = ['Gene,Allele,Guideline,URL'];
    _.forEach(original, (o) => {csv.push(o.genesymbol + ',' + o.allele_name + ',' + o.guideline_name + ',' + o.guideline_url)});
    fs.writeFile(process.argv[3], csv.join('\n'), (e) => {
      if (e) console.log(e);
      console.log('Done');
    });
  });
