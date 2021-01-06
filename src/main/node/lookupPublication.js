const axios = require('axios');
const db = require('./db');

/**
 * This script will take one argument, the PMID of a publication, query the PharmGKB API for Literature data, and then 
 * write the information to the publication table.
 */
const updatePublication = async (pmid) => {
  if (!process.env.PGHOST) {
    throw new Error('No DB host configured');
  }
  console.log(`writing to DB: ${process.env.PGHOST}`);

  const response = await axios.get(
    'https://api.pharmgkb.org/v1/preview/data/literature/',
    {params: {view: 'base', "crossReferences.resourceId": pmid, 'crossReferences.resource': 'PubMed'}});
  const lit = response.data.data[0];
  lit.pmid = pmid;
  lit.doi = getXrefId(lit.crossReferences, 'DOI');
  lit.pmcid = getXrefId(lit.crossReferences, 'PubMed Central');

  await db.none(`
insert into publication(title, authors, journal, month, year, pmid, doi, pmcid) 
values ($(title), $(authors), $(journal), $(month), $(year), $(pmid), $(doi), $(pmcid)) on conflict (pmid) do update
set title=excluded.title, authors=excluded.authors, journal=excluded.journal, month=excluded.month, year=excluded.year, doi=excluded.doi, pmcid=excluded.pmcid
    `,
    lit
  )
};

const getXrefId = (xrefs = [], resource) => {
  const rez = xrefs.filter((x) => x.resource === resource);
  if (rez) return rez[0]?.resourceId;
  return null;
}

try {
  updatePublication(process.argv[2]);
}
catch (err) {
  console.error('Error loading publication data', err);
  process.exit(1);
}
