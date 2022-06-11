import {execSync}  from 'child_process';
import yargs from 'yargs';
import {hideBin} from 'yargs/helpers';
import {existsSync, mkdirSync, readFileSync, unlinkSync} from 'fs';
import {PutObjectCommand, S3Client} from "@aws-sdk/client-s3";
import {basename} from 'path';


const argv = yargs(hideBin(process.argv))
    .option('b', {
        alias: 'base-version',
        description: 'Use base version',
    })
    .option('h', {
        alias: 'host',
        description: 'Postgres host',
        type: 'string',
    })
    .option('z', {
        alias: 'archive',
        description: 'gzip dump files'
    })
    .help()
//    .alias('help', 'h')
    .argv;

const fullVersion = execSync('git describe --tags', { encoding: 'utf-8'});
let version = fullVersion.trim();
if (argv.b) {
    version = /^(v\d+\.\d+\.\d+).*/.exec(fullVersion)[1];
}

console.log(`Version ${version}`);


const pgdumpArgs = [
    'cpic -U cpic --schema=cpic -no-owner ',
];
const env = {...process.env};

if (argv.h) {
    pgdumpArgs.push(`-h ${argv.h}`);
} else if (process.env.POSTGRES_HOST) {
    pgdumpArgs.push(`-h ${process.env.POSTGRES_HOST}`);
}

if (process.env.CPIC_PASS) {
    env.PGPASSWORD = process.env.CPIC_PASS;
}


const baseName = 'cpic_db_dump';
const dbFile = `out/${baseName}-${version}.sql`;
const insertsFile = `out/${baseName}-${version}_inserts.sql`;

if (!existsSync('out')) {
    mkdirSync('out');
}
console.log('\nDumping db...');
_exec(`pg_dump ${pgdumpArgs.join(' ')} --no-privileges -f ${dbFile}`, env);
console.log('\nDumping inserts only...');
_exec(`pg_dump ${pgdumpArgs.join(' ')} --data-only --column-inserts -f ${insertsFile}`, env);

if (argv.z) {
    console.log('\nArchiving...');
    const dbGzFile = `${dbFile}.gz`;
    const insertsGzFile = `${insertsFile}.gz`;
    _deleteIfExists(dbGzFile);
    _deleteIfExists(insertsGzFile);
    _exec(`gzip ${dbFile}`);
    _exec(`gzip ${insertsFile}`);
    if (argv.u) {
        console.log('\nUploading to S3...');
        const client = new S3Client({region: 'us-west-2'});
        await _uploadToS3(client, dbGzFile);
        await _uploadToS3(client, insertsGzFile);
    }
}


console.log('\nDone.');


function _exec(cmd, env) {
    const rez = execSync(cmd, {encoding: 'utf-8', env});
    if (rez) {
        console.log(rez);
    }
}

function _deleteIfExists(path) {
    if (existsSync(path)) {
        console.log(`Deleting old ${path}`);
        unlinkSync(`${path}`);
    }
}

async function _uploadToS3(client, path) {
    const baseFileName = basename(path);
    const command = new PutObjectCommand({
        Bucket: 'files.cpicpgx.org',
        Key: `data/database/${baseFileName}`,
        Body: readFileSync(path),
    });
    try {
        await client.send(command);
        console.log(`Uploaded to ${baseFileName}`);
    } catch (error) {
        console.log(error);
    }
}
