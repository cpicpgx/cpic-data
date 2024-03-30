import pg from 'pg'

const host = process.env.PGHOST ?? 'localhost';
const port = process.env.PGPORT ?? 5432;
const adminUser = process.env.PGUSER ?? 'postgres';
const adminPwd = process.env.PGPASSWORD ?? '';
const cpicApiPwd = process.env.CPIC_API_PASS ?? 'CHANGE_ME';
const cpicAuth = process.env.CPIC_AUTH_USER ?? 'auth';
const cpicAuthPwd = process.env.CPIC_AUTH_PASS ?? cpicApiPwd;
const alreadyExists = / already exists$/;


const cpicDbAsPostgres = new pg.Client({
  host,
  port,
  user: adminUser,
  password: adminPwd,
  database: 'cpic',
});
await cpicDbAsPostgres.connect();

console.log('Creating cpic_api user...');
try {
  await cpicDbAsPostgres.query(`create role cpic_api with login password \'${cpicApiPwd}\'`);
} catch (ex) {
  if (alreadyExists.test(ex.message)) {
    console.warn('cpic_api already exists, skipping');
  } else {
    throw ex;
  }
}

console.log('Granting to cpic_api user...');
await cpicDbAsPostgres.query(`
    grant usage on schema cpic to cpic_api;
    grant all privileges on all tables in schema cpic to cpic_api;
    grant usage,select on sequence cpic.cpic_id to cpic_api;
    grant execute on all functions in schema cpic to cpic_api;
`);


console.log('Creating web_anon role...');
try {
  await cpicDbAsPostgres.query('create role web_anon');
} catch (ex) {
  if (alreadyExists.test(ex.message)) {
    console.warn('web_anon already exists, skipping');
  } else {
    throw ex;
  }
}

console.log('Granting to web_anon role...');
await cpicDbAsPostgres.query(`
  grant usage on schema cpic to web_anon;
  grant select on all tables in schema cpic to web_anon;
  grant execute on all functions in schema cpic to web_anon;
`);


console.log('Creating auth role...');
try {
  await cpicDbAsPostgres.query(`create user auth with password '${cpicAuthPwd}'`);
} catch (ex) {
  if (alreadyExists.test(ex.message)) {
    console.warn('auth already exists, skipping');
  } else {
    throw ex;
  }
}

console.log('Creating auth schema...')
await cpicDbAsPostgres.query('create schema if not exists authorization auth');
cpicDbAsPostgres.end();

const cpicDbAsAuth = new pg.Client({
  host,
  port,
  user: cpicAuth,
  password: cpicAuthPwd,
  database: 'cpic',
});
await cpicDbAsAuth.connect();

await cpicDbAsAuth.query(`
CREATE TABLE if not exists auth.accounts
(
    id                   SERIAL,
    compound_id          VARCHAR(255) NOT NULL,
    user_id              INTEGER NOT NULL,
    provider_type        VARCHAR(255) NOT NULL,
    provider_id          VARCHAR(255) NOT NULL,
    provider_account_id  VARCHAR(255) NOT NULL,
    refresh_token        TEXT,
    access_token         TEXT,
    access_token_expires TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE if not exists auth.sessions
(
    id            SERIAL,
    user_id       INTEGER NOT NULL,
    expires       TIMESTAMPTZ NOT NULL,
    session_token VARCHAR(255) NOT NULL,
    access_token  VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE if not exists auth.users
(
    id             SERIAL,
    name           VARCHAR(255),
    email          VARCHAR(255),
    email_verified TIMESTAMPTZ,
    image          VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE if not exists auth.verification_requests
(
    id         SERIAL,
    identifier VARCHAR(255) NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires    TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX if not exists compound_id   ON auth.accounts(compound_id);
CREATE INDEX if not exists provider_account_id  ON auth.accounts(provider_account_id);
CREATE INDEX if not exists provider_id          ON auth.accounts(provider_id);
CREATE INDEX if not exists user_id              ON auth.accounts(user_id);
CREATE UNIQUE INDEX if not exists session_token ON auth.sessions(session_token);
CREATE UNIQUE INDEX if not exists access_token  ON auth.sessions(access_token);
CREATE UNIQUE INDEX if not exists email         ON auth.users(email);
CREATE UNIQUE INDEX if not exists token         ON auth.verification_requests(token);
`);
cpicDbAsAuth.end();
