const fs = require('fs');
const path = require('path');
const alleles = require('./getAlleles');
const guidelines = require('./getGuidelines');
const pairs = require('./getPairs');
const pub = require('./getPublications');

const outDirectoryPath = path.join(__dirname, '..', '..', '..', 'out');
if (!fs.existsSync(outDirectoryPath)) {
  fs.mkdirSync(outDirectoryPath);
}

alleles.getAlleles(outDirectoryPath);
guidelines.getGuidelines(outDirectoryPath);
pairs.getPairs(outDirectoryPath);
pub.getPublications(outDirectoryPath);
