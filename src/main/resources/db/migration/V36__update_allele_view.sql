-- This view is being created mainly for use on the CPIC website. It is meant to show a list of alleles with
-- clinical functional status assigned and that are associated, through a gene, with a guideline.

create view allele_guideline as
select
    a.genesymbol,
    a.name as allelename,
    (
        select jsonb_agg(distinct jsonb_build_object('name', g.name, 'url', g.url))
        from pair p join guideline g on (g.id=p.guidelineid) where a.genesymbol=p.genesymbol
    ) guidelines
from
    allele a
where
    a.clinicalfunctionalstatus is not null
group by a.genesymbol, a.name;

comment on view allele_guideline is 'A list of all alleles and the guidelines they are associated with';
comment on column allele_guideline.genesymbol is 'The gene the allele is part of';
comment on column allele_guideline.allelename is 'The name of the allele';
comment on column allele_guideline.guidelines is 'Guidelines that the gene are associated with';
