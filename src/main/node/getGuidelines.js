const axios = require('axios');
const cpicapi = require('./cpicapi');
const fs = require('fs');

const defaultFilename = 'guidelines.json';

exports.getGuidelines = (path) => {
  const uri = cpicapi.apiUrl('/guideline');
  const jsonFile = path + "/" + defaultFilename;

  axios.get(
    uri,
    {params: {select: 'name,url,gene(symbol),drug(name)', order: 'name'}},
  )
    .then((r) => {
      fs.writeFile(jsonFile, JSON.stringify(r.data, null, 2), (e) => {
        if (e) console.log(e);
        console.log(`Done writing ${jsonFile}`);
      });
    });
}
