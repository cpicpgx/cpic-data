insert into change_log
select distinct date(now()) as "date", 'GENE_CDS' as "type", genesymbol as entityid, 'added gene symbol as a prefix to the phenotype column values' as "note", 1 as "version", null as deployedrelease
from gene_result where consultationtext is not null order by 1;
