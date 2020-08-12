const alleles = require('./getAlleles');
const guidelines = require('./getGuidelines');
const pairs = require('./getPairs');
const pub = require('./getPublications');

const path = process.argv[2];

alleles.getAlleles(path);
guidelines.getGuidelines(path);
pairs.getPairs(path);
pub.getPublications(path);
