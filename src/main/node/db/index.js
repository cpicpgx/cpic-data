const pgp = require('pg-promise')({schema: 'cpic'});

const host = process.env.PGHOST || 'localhost';
const dbname = process.env.PGDATABASE || 'cpic';
const user = host === 'localhost' ? 'cpic' : `cpic:${process.env.PGPASS}`;

const cn = `postgres://${user}@${host}:5432/${dbname}`;
const db = pgp(cn);

module.exports = db;