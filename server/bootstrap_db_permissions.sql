-- we expect that a "cpic" role already exists with LOGIN permission

-- the schema that will actually hold the data
create schema if not exists cpic;
grant usage on schema cpic to cpic;
grant all privileges on schema cpic to cpic;

alter role cpic set search_path = core;

drop schema if exists public;
