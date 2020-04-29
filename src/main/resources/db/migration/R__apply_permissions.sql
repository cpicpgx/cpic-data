-- the role used for actual connection and CRUD
grant select,insert,update,delete on all tables in schema public to cpic_api;
grant usage,select on sequence cpic_id to cpic_api;
-- the role used for read-only queries
grant select on all tables in schema public to web_anon;

grant select on gene_drug_pair to web_anon;
grant select on data_progress to web_anon;
grant select on diplotype_view to web_anon;
grant select on allele_guideline_view to web_anon;
grant select on population_frequency_view to web_anon;
grant select on file_status to web_anon;
