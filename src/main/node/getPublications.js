const axios = require('axios');
const cpicapi = require('./cpicapi');
const fs = require('fs');

const defaultFilename = 'publications.json';
const pmids = ['21270786','24479687','27441996', '27026620', '27864205', '31562822'];

exports.getPublications = (path) => {
  const uri = cpicapi.apiUrl('/guideline');
  const filePath = `${path}/${defaultFilename}`;

  axios.get(
    uri,
    {params: {select: 'id,name,url,publication(*)', order: 'name'}},
  )
    .then((r) => {
      fs.writeFile(filePath, JSON.stringify(r.data, null, 2), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${filePath}`);
      });
    });

  pmids.forEach((pmid) => {
    axios.get(cpicapi.apiUrl('/publication'), {params: {pmid: `eq.${pmid}`}})
      .then((r) => {
        const pmidFile = `${path}/${pmid}.json`;
        fs.writeFile(pmidFile, JSON.stringify(r.data, null, 2), (e) => {
          if (e) console.log(e);
          console.log(`Done writing ${pmidFile}`);
        });
      });
  });
};
