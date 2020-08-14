-- the role used for actual connection and CRUD
grant usage on schema cpic to cpic_api;
grant usage on schema cpic to web_anon;
grant select,insert,update,delete on all tables in schema cpic to cpic_api;
grant usage,select on sequence cpic_id to cpic_api;
-- the role used for read-only queries
grant select on all tables in schema cpic to web_anon;
grant execute on all functions in schema cpic to web_anon;

grant select on data_progress to web_anon;
grant select on diplotype to web_anon;
grant select on allele_guideline_view to web_anon;
grant select on population_frequency_view to web_anon;
grant select on file_status to web_anon;
grant select on recommendation_view to web_anon;
grant select on test_alert_view to web_anon;
grant select on pair_view to web_anon;
grant select on change_log_view to web_anon;
