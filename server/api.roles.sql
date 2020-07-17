-- cpic_api is the role that postgrest connects with
create user cpic_api with password 'cpicapi';
grant usage on schema cpic to cpic_api;
grant usage on schema cpic to web_anon;

-- the web_anon role is the set of permissions for anonymous read usage of the data
create role web_anon;
grant web_anon to cpic_api;
