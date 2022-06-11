import pgPromise from 'pg-promise';

const host = process.env.POSTGRES_HOST ?? 'localhost';
const port = process.env.POSTGRES_PORT ?? 5432;
const adminUser = process.env.POSTGRES_USER ?? 'postgres';
const adminPwd = process.env.POSTGRES_PASSWORD ?? '';
const cpicPwd = process.env.CPIC_PASS ?? 'CHANGE_ME';
const cpicApiPwd = process.env.CPIC_API_PASS ?? cpicPwd;

const pgp = pgPromise({});
const pgDb = pgp({
    host,
    port,
    user: adminUser,
    password: adminPwd,
    database: 'postgres',
});

console.log('Creating cpic user...');
await pgDb.none(`create user cpic createdb login encrypted password '${cpicPwd}'`);
console.log('Creating cpic database...');
await pgDb.none('create database cpic owner cpic');


const cpicDbAsCpic = pgp({
    host,
    port,
    user: 'cpic',
    password: cpicPwd,
    database: 'cpic',
});

console.log('Creating cpic schema...');
await cpicDbAsCpic.none('create schema cpic');
await cpicDbAsCpic.none('alter role cpic set search_path=\'cpic\'');

const cpicDbAsPostgres = pgp({
    host,
    port,
    user: adminUser,
    password: adminPwd,
    database: 'cpic',
});
console.log('Creating cpic_api user...');
await cpicDbAsPostgres.none(`create user cpic_api with password '${cpicApiPwd}'`);
await cpicDbAsPostgres.none('grant usage on schema cpic to cpic_api');
console.log('Creating web_anon role');
await cpicDbAsPostgres.none('create role web_anon');
await cpicDbAsPostgres.none('grant usage on schema cpic to web_anon');
