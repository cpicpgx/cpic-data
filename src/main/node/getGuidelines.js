const axios = require('axios');
const cpicapi = require('./cpicapi');
const fs = require('fs');

const uri = cpicapi.apiUrl('/guideline');
console.log(uri);

axios.get(
  uri,
  {params: {select: 'name,url,gene(symbol),drug(name)', order: 'name'}},
)
.then((r) => {
  fs.writeFile(process.argv[2], JSON.stringify(r.data, null, 2), (e) => {
    if (e) console.log(e);
    console.log('Done');
  });
});
