import pgPromise from 'pg-promise';
import {exec} from 'child_process';

const pgp = pgPromise({});
const pgDb = pgp({
    database: 'postgres',
    port: 5432,
    user: 'postgres', // any admin user
    password: process.env.POSTGRES_PASSWORD ?? '',
});

console.log('Creating cpic role...');
await pgDb.none('create role cpic createdb login');
console.log('Creating cpic database...');
await pgDb.none('create database cpic owner cpic');


const cpicDb = pgp({
    database: 'cpic',
    port: 5432,
    user: 'cpic',
    password: process.env.CPIC_PASS ?? '',
});

console.log('Creating cpic schema...');
await cpicDb.none('create schema cpic');
await cpicDb.none('alter role cpic set search_path=\'cpic\'');
