drop function cpic.pharmgkb_guideline_alleles;

create function cpic.pharmgkb_guideline_alleles(pharmgkbdrugid text)
    returns table(genesymbol text, chr text, lookupmethod text, alleles text[]) as
$$
select
    a.genesymbol,
    g.chr,
    g.lookupmethod,
    array_agg(distinct a.name order by a.name) as alleles
from allele a join gene g on a.genesymbol=g.symbol
where
    (a.clinicalfunctionalstatus is not null or g.lookupmethod='ALLELE_STATUS')
  and a.genesymbol in (
    select distinct jsonb_object_keys(p.lookupkey) as genes
    from recommendation p join guideline g on p.guidelineid = g.id join drug d on p.drugid=d.drugid
    where d.pharmgkbid=pharmgkbdrugid
)
group by a.genesymbol, g.chr, g.lookupmethod order by a.genesymbol
$$ language SQL stable;
