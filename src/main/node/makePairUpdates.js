#!/bin/node
const axios = require('axios');
const log = require('./log');
const db = require('./db');
const _ = require('lodash');

/**
 * This script will take current pair data from the CPIC DB, submit it to the PharmGKB API, and apply any found changes
 * back to the CPIC DB. This can be safely be run multiple times since any changes will only be applied once and
 * subsequent runs will no longer include that change.
 *
 * __Requirements__
 * Make sure you set the following environment variables. Otherwise, it will default to trying to use localhost
 * 1. PGKBAPI = the domain (and possible port) to use for PharmGKB, default is "localhost.pharmgkb.org:8543"
 * 2. PGHOST = the host of the CPIC DB, default is "localhost"
 * 3. PGPASS = the password for the "cpic" DB account, default is blank
 * 4. PGKBAPI = the hostname of the PharmGKB API to use, will default to localhost
 */

const pgkbApi = process.env.PGKBAPI || 'localhost.pharmgkb.org:8543';

const queryCurrentPairs = async () => {
    return await db.many(`
        select json_build_object('drugid', d.drugid, 'name', d.name, 'pharmgkbid', d.pharmgkbid) as drug,p.genesymbol,
               p.pgkbcalevel, p.pgxtesting, p.pairid
        from pair p join drug d on p.drugid=d.drugid where p.removed is false
    `);
};

const requestUpdates = async (body) => {
    const pgkbApiUrl = `https://${pgkbApi}/v1/collaborator/cpic/pair`;
    log.debug(`Checking ${pgkbApiUrl}`);
    try {
        const response = await axios.post(
            pgkbApiUrl,
            body
        );
        return response.data;
    } catch (err) {
        log.error(err.message);
    }
};

const hasCaChange = (change) => change.hasOwnProperty('pairid') && change.hasOwnProperty('pgkbcalevel');
const hasTestingChange = (change) => change.hasOwnProperty('pairid') && change.hasOwnProperty('pgxtesting');
const mapNoneToNull = (value) => {
    if (!value || _.trim(value) === 'none') {
        return null;
    } else {
        return _.trim(value);
    }
}
const mapNullToNone = (value) => value || 'none';

const writeChangeNote = async (message) => {
    log.info(message);
    return db.none(
        `insert into change_log(date, type, note) values (current_date, 'PAIR', $(message))`,
        {message},
    );
}

const updateCaLevel = async ({pairid, pgkbcalevel: pgkbcalevelIn, name}) => {
    const currentPair = await db.one(`select genesymbol, drugname, pgkbcalevel from pair_view where pairid=$(pairid)`, {pairid});
    const pgkbcalevel = mapNoneToNull(pgkbcalevelIn);
    if (currentPair.pgkbcalevel !== pgkbcalevel) {
        await db.none(
            `update pair
             set pgkbcalevel=$(pgkbcalevel)
             where pairid = $(pairid)`,
            {pgkbcalevel, pairid},
        );
        return writeChangeNote(`[${currentPair.drugname}-${currentPair.genesymbol}] PharmGKB level changed from ${mapNullToNone(currentPair.pgkbcalevel)} to ${mapNullToNone(pgkbcalevel)}`);
    } else {
        log.debug(`Skipping ${name} since CA level already matches`);
        return Promise.resolve();
    }
};

const updateTesting = async ({pairid, pgxtesting: pgxtestingIn, name}) => {
    const currentPair = await db.one(`select genesymbol, drugname, pgxtesting from pair_view where pairid=$(pairid)`, {pairid});
    const pgxtesting = mapNoneToNull(pgxtestingIn);
    if (currentPair.pgxtesting !== pgxtesting) {
        await db.none(
            `update pair
             set pgxtesting=$(pgxtesting)
             where pairid = $(pairid)`,
            {pgxtesting, pairid},
        );
        return writeChangeNote(`[${currentPair.drugname}-${currentPair.genesymbol}] PGx testing level changed from ${mapNullToNone(currentPair.pgxtesting)} to ${mapNullToNone(pgxtesting)}`);
    } else {
        log.debug(`Skipping ${name} since PGx testing already matches`);
        return Promise.resolve();
    }
};

const processChanges = async (changes) => {
    await Promise.all(changes.filter(hasCaChange).map(updateCaLevel));
    await Promise.all(changes.filter(hasTestingChange).map(updateTesting));
}

try {
    log.info('Checking for pair updates');
    log.debug(`Writing to DB: ${process.env.PGHOST || 'localhost'}`);
    queryCurrentPairs()
        .then(async (data) => {
            log.debug(`${data?.length || 0} pairs in CPIC`)
            const changes = await requestUpdates(data);
            const updates = _.filter(changes?.data, (c) => !!c.pairid);
            log.debug(`${updates?.length || 0} changes found in PGKB`)
            await processChanges(updates);
        })
        .catch((err) => log.error('Error requesting updates', err))
        .finally(() => process.exit(0));
} catch (err) {
    log.error('Error updating pairs', err);
    process.exit(1);
}
