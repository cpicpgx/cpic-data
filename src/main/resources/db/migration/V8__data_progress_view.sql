create view data_progress as
select 'Gene' as object_class, 'Allele Definition Data' as data_type, genesymbol as object_name, count(*) as n
from allele_definition
group by genesymbol
union all
select 'Gene' as object_class, 'Allele Functionality Data' as data_type, genesymbol as object_name, count(*) as n
from function_reference r
         join allele a on r.alleleid = a.id
group by genesymbol
union all
select 'Gene' as object_class, 'Diplotype to Phenotype Data' as data_type, genesymbol as object_name, count(*) as n
from diplotype_view
group by genesymbol
union all
select 'Gene' as object_class, 'Frequency Data' as data_type, genesymbol as object_name, count(*) as n
from allele_frequency f
         join allele a on f.alleleid = a.id
group by genesymbol
union all
select 'Gene' as object_class, 'Gene CDS Data' as data_type, genesymbol as object_name, count(*) as n
from gene_phenotype
where consultationtext is not null
group by genesymbol
union all
select 'Gene' as object_class, 'Gene Phenotype Data' as data_type, g.genesymbol as object_name, count(*) as n
from gene_phenotype g
         join phenotype_function pf on g.id = pf.phenotypeid
group by genesymbol
union all
select 'Gene' as object_class, 'PharmVar Allele IDs' as data_type, a.genesymbol as object_name, count(*) as n
from allele_definition a
where a.pharmvarid is not null
group by a.genesymbol
union all
select 'Drug' as object_class, 'Flowcharts' as data_type, name as object_name, count(*) as n from drug where flowchart is not null group by name
union all
select 'Drug' as object_class, 'Table 2 Recommendations' as data_type, d.name as object_name, count(distinct r.id) as n from recommendation r join drug d on r.drugid = d.drugid group by d.name
union all
select 'Drug' as object_class, 'Drug Test Alerts' as data_type, d.name as object_name, count(distinct t.id) as n from test_alerts t join drug d on t.drugid = d.drugid group by d.name
union all
select 'Drug' as object_class, 'Guideline' as data_type, d.name as object_name, count(distinct g.id) as n from guideline g join pair p on g.id = p.guidelineid join drug d on p.drugid = d.drugid group by d.name
;
