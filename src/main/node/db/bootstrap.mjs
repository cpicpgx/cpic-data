import pg from 'pg'

const host = process.env.PGHOST ?? 'localhost';
const port = process.env.PGPORT ?? 5432;
const adminUser = process.env.PGUSER ?? 'postgres';
const adminPwd = process.env.PGPASSWORD ?? '';
const cpicPwd = process.env.CPIC_PASS ?? 'CHANGE_ME';
const alreadyExists = / already exists$/;

const pgDb = new pg.Client({
    host,
    port,
    user: adminUser,
    password: adminPwd,
    database: 'postgres',
});
await pgDb.connect();

console.log('Creating cpic user...');
try {
    await pgDb.query(`create role cpic with createdb login password \'${cpicPwd}\'`);
} catch (e) {
    if (alreadyExists.test(e.message)) {
        console.warn('cpic role exists, skipping');
    } else {
        throw e;
    }
}
console.log('Creating cpic database...');
try {
    await pgDb.query('create database cpic owner cpic');
} catch (e) {
    if (alreadyExists.test(e.message)) {
        console.warn('cpic db exists, skipping');
    } else {
        throw e;
    }
}
await pgDb.end();


const cpicDbAsCpic = new pg.Client({
    host,
    port,
    user: 'cpic',
    password: cpicPwd,
    database: 'cpic',
});
cpicDbAsCpic.connect();

console.log('Creating cpic schema...');
await cpicDbAsCpic.query('create schema if not exists cpic');
await cpicDbAsCpic.query('alter role cpic set search_path=\'cpic\'');
await cpicDbAsCpic.end();