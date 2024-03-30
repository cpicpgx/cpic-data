import { Pool } from 'pg'

const pool = new Pool({
  host: process.env.PGHOST || 'localhost',
  database: process.env.PGDATABASE || 'cpic',
  user: 'cpic',
  // to specify password use env.PGPASSWORD
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
  application_name: 'cpic-data node',
});

export const query = (text, params, callback) => {
  return pool.query(text, params, callback);
}
