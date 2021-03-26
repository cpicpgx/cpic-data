const fs = require('fs');
const path = require('path');
const guidelines = require('./getGuidelines');
const pairs = require('./getPairs');
const pub = require('./getPublications');

const outDirectoryPath = path.join(__dirname, '..', '..', '..', 'out');
if (!fs.existsSync(outDirectoryPath)) {
  fs.mkdirSync(outDirectoryPath);
}

guidelines.getGuidelines(outDirectoryPath);
pairs.getPairs(outDirectoryPath);
pub.getPublications(outDirectoryPath);
