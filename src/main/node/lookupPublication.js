const axios = require('axios');

/**
 * This script will take one argument, the PMID of a publication, query the PharmGKB API for Literature data, and then 
 * return a TSV line representing that publication.
 */

const pmid = process.argv[2];

axios.get(
  'https://api.pharmgkb.org/v1/preview/data/literature/',
  {params: {view: 'base', "crossReferences.resourceId": pmid, 'crossReferences.resource': 'PubMed'}})
  .then((r) => {
    const lit = r.data.data[0];
    const authors = '{' + lit.authors.map((a) => `"${a}"`).join(",") + '}';
    const fields = [
      lit.title,
      authors,
      lit.journal,
      lit.month,
      '',
      '',
      lit.year,
      pmid
    ];
    console.log('COPY publication (title, authors, journal, month, page, volume, year, pmid) FROM stdin;');
    console.log(fields.join('\t'));
    console.log('\\.');
  });
