const pgp = require('pg-promise')({schema: 'cpic'});

const cn = `postgres://cpic@${process.env.PGHOST}:5432/cpic`;
const db = pgp(cn);

module.exports = db;