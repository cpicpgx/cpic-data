#!/bin/node
const axios = require('axios');
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
  const response = await axios.post(
    `https://${pgkbApi}/v1/collaborator/cpic/pair`,
    body
  );
  return response.data;
};

const processChanges = async (changes) => {
  await Promise.all(changes.filter(c => !!c.pairid && !!c.pgkbcalevel).map(({pgkbcalevel, pairid}) => {
    console.log(`Updating ${pairid} to CA ${pgkbcalevel}`);
    return db.none(`update pair
             set pgkbcalevel=$(pgkbcalevel)
             where pairid = $(pairid)`, {pgkbcalevel, pairid});
  }));

  await Promise.all(changes.filter(c => !!c.pairid && !!c.pgxtesting).map(({pgxtesting, pairid}) => {
    console.log(`Updating ${pairid} to PGx ${pgxtesting}`);
    return db.none(`update pair
             set pgxtesting=$(pgxtesting)
             where pairid = $(pairid)`, {pgxtesting, pairid});
  }));
}

try {
   queryCurrentPairs()
    .then(async (data) => {
      console.log(`${data?.length || 0} pairs in CPIC`)
      const changes = await requestUpdates(data);
      const updates = _.filter(changes?.data, (c) => !!c.pairid);
      console.log(`${updates?.length || 0} changes found in PGKB`)
      await processChanges(updates);
    })
    .catch((err) => console.err('Error requesting updates', err))
    .finally(() => process.exit(0));
} catch (err) {
  console.error('Error updating pairs', err);
  process.exit(1);
}
