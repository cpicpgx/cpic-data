set search_path = 'cpic';
\pset footer off

-- Size of the database on disk
select pg_database_size('cpic') as "Database Size (in bytes)";

-- Count of tables
select count(*) as "Table Count" from information_schema.tables where table_schema='cpic' and table_type='BASE TABLE';

-- Count of all rows in all tables
select sum(n_live_tup) as "Total Row Count" from pg_stat_user_tables where schemaname='cpic';

-- Count genes with allele definitions
select count(distinct genesymbol) as "Genes with allele definitions" from allele_definition;

-- Count genes with function assignments
select count(distinct genesymbol) as "Genes with function" from allele where clinicalfunctionalstatus is not null;

-- Count genes with allele frequencies
select count(distinct a.genesymbol) as "Genes with frequencies" from allele_frequency f join allele a on f.alleleid = a.id;

-- Count genes with CDS test alerts
select count(x.gene) as "Genes with CDS test alerts" from (select distinct unnest(genes) as gene from test_alert) x;

-- Count guidelines with recommendations
--   need to count the files in the "recommendations" folder in cpic-support-files

-- Count drugs with recommendations
select count(distinct drugid) as "Drugs with recommendations" from recommendation;

-- Count the test alert files
--    need to count the files in the "test_alert" folder in cpic-support-files

-- Count the gene-drug pairs
select count(*) as "Gene-Drug pairs with levels assigned" from pair;
