const cpicapi = require('./cpicapi');
const axios = require('axios');
const _ = require('lodash');

const geneUrl = cpicapi.apiUrl('/gene');
const hgncUrl = 'http://rest.genenames.org/fetch/symbol/';

axios
  .get(geneUrl, {params: {hgncid: 'is.null', select: 'symbol'}})
  .then((r) => {
    r.data.forEach(g => {
      axios
        .get(hgncUrl + g.symbol, {})
        .then((h) => {
          const hgnc = _.get(h, 'data.response.docs[0]', {});
          const hgncId = _.get(hgnc, 'hgnc_id');
          const ensemblId = _.get(hgnc, 'ensembl_gene_id');
          const ncbiId = _.get(hgnc, 'entrez_id');
          console.log(`update gene set hgncid='${hgncId}',ncbiid='${ncbiId}',ensemblid='${ensemblId}' where symbol='${g.symbol}';`);
        });
    });
  });
