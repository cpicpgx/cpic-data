const log = require('loglevel');
const prefix = require('loglevel-plugin-prefix');

prefix.reg(log);
log.setLevel(log.levels.INFO);
prefix.apply(log);
module.exports = log;
