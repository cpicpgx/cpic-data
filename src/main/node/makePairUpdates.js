#!/bin/node
const fs = require('fs');
const path = process.argv[2];

const pairs = JSON.parse(fs.readFileSync(path, 'UTF-8'));
pairs.forEach((pair) => {
  if (pair.pairid) {
    if (pair.pgkbcalevel) {
      console.log(`update pair set pgkbcalevel='${pair.pgkbcalevel}' where pairid=${pair.pairid};`);
    }
    if (pair.pgxtesting) {
      console.log(`update pair set pgxTesting='${pair.pgxtesting}' where pairid=${pair.pairid};`);
    }
  }
});
