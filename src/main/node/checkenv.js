const log = require('./log');

/**
 * This file exists to verify that the env vars are available in your environment
 */

require('dotenv').config();

log.info(process.env);
