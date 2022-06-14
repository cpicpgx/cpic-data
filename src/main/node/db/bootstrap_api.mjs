import pgPromise from 'pg-promise';

const host = process.env.POSTGRES_HOST ?? 'localhost';
const port = process.env.POSTGRES_PORT ?? 5432;
const adminUser = process.env.POSTGRES_USER ?? 'postgres';
const adminPwd = process.env.POSTGRES_PASSWORD ?? '';
const cpicApiPwd = process.env.CPIC_API_PASS ?? 'CHANGE_ME';
const cpicAuth = process.env.CPIC_AUTH_USER ?? 'auth';
const cpicAuthPwd = process.env.CPIC_AUTH_PASS ?? cpicApiPwd;


const pgp = pgPromise({});

const cpicDbAsPostgres = pgp({
  host,
  port,
  user: adminUser,
  password: adminPwd,
  database: 'cpic',
});

console.log('Creating cpic_api user...');
try {
  await cpicDbAsPostgres.none(`create user cpic_api with password '${cpicApiPwd}'`);
} catch (ex) {
  console.error(ex.message);
}

console.log('Granting to cpic_api user...');
await cpicDbAsPostgres.multiResult(`
    grant usage on schema cpic to cpic_api;
    grant all privileges on all tables in schema cpic to cpic_api;
    grant usage,select on sequence cpic.cpic_id to cpic_api;
    grant execute on all functions in schema cpic to cpic_api;
`);


console.log('Creating web_anon role...');
try {
  await cpicDbAsPostgres.none('create role web_anon');
} catch (ex) {
  console.error(ex.message);
}

console.log('Granting to web_anon role...');
await cpicDbAsPostgres.none(`
  grant usage on schema cpic to web_anon;
  grant select on all tables in schema cpic to web_anon;
  grant execute on all functions in schema cpic to web_anon;
`);


console.log('Creating auth role...');
try {
  await cpicDbAsPostgres.none(`create user auth with password '${cpicAuthPwd}'`);
} catch (ex) {
  console.error(ex.message);
}

console.log('Creating auth schema...')
await cpicDbAsPostgres.none('create schema authorization auth');
const cpicDbAsAuth = pgp({
  host,
  port,
  user: cpicAuth,
  password: cpicAuthPwd,
  database: 'cpic',
});
await cpicDbAsAuth.multiResult(`
CREATE TABLE auth.accounts
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

CREATE TABLE auth.sessions
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

CREATE TABLE auth.users
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

CREATE TABLE auth.verification_requests
(
    id         SERIAL,
    identifier VARCHAR(255) NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires    TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX compound_id   ON auth.accounts(compound_id);
CREATE INDEX provider_account_id  ON auth.accounts(provider_account_id);
CREATE INDEX provider_id          ON auth.accounts(provider_id);
CREATE INDEX user_id              ON auth.accounts(user_id);
CREATE UNIQUE INDEX session_token ON auth.sessions(session_token);
CREATE UNIQUE INDEX access_token  ON auth.sessions(access_token);
CREATE UNIQUE INDEX email         ON auth.users(email);
CREATE UNIQUE INDEX token         ON auth.verification_requests(token);
`);
