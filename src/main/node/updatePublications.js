const cpicapi = require('./cpicapi');
const axios = require('axios');
const _ = require('lodash');

const pubUrl = cpicapi.apiUrl('/publication');
const ncbiUrl = 'https://www.ncbi.nlm.nih.gov/pmc/utils/idconv/v1.0/?format=json&email=ops@cpicpgx.org&tool=cpicpgx&ids=';

axios
  .get(pubUrl, {params: {pmcid: 'is.null', select: 'pmid'}})
  .then((r) => {
    r.data.forEach(g => {
      axios
        .get(ncbiUrl + g.pmid, {})
        .then((h) => {
          const pubmed = _.get(h, 'data.records[0]', {});
          const pmcid = _.get(pubmed, 'pmcid');
          const doi = _.get(pubmed, 'doi');
          console.log(`update publication set pmcid='${pmcid}',doi='${doi}' where pmid='${g.pmid}';`);
        });
    });
  });
