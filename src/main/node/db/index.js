const pgp = require('pg-promise')({schema: 'cpic'});

const user = process.env.PGHOST === 'localhost' ? 'cpic' : `cpic:${process.env.PGPASS}`;
const cn = `postgres://${user}@${process.env.PGHOST}:5432/cpic`;
const db = pgp(cn);

module.exports = db;