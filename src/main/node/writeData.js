const alleles = require('./getAlleles');
const guidelines = require('./getGuidelines');
const pairs = require('./getPairs');

const path = process.argv[2];

alleles.getAlleles(path);
guidelines.getGuidelines(path);
pairs.getPairs(path);
