create function cpic.pharmgkb_guideline_alleles(pharmgkbdrugid text)
    returns table(genesymbol text, chr text, alleles text[]) as
$$
select
    a.genesymbol,
    g.chr,
    array_agg(distinct a.name order by a.name) as alleles
from allele a join gene g on a.genesymbol=g.symbol
where
    a.clinicalfunctionalstatus is not null
  and a.genesymbol in (
    select distinct jsonb_object_keys(p.lookupkey) as genes
    from recommendation p join guideline g on p.guidelineid = g.id join drug d on g.id = d.guidelineid
    where d.pharmgkbid=pharmgkbdrugid
)
group by a.genesymbol, g.chr order by a.genesymbol
$$ language SQL stable;

comment on function cpic.pharmgkb_guideline_alleles(guidelineid text)
    is 'For a given PharmGKB guideline ID, list the alleles that are options for looking up recommendations';


create function cpic.pharmgkb_guideline_recommendation(guidelineidArg text, lookupkeyArg text) returns
    table
    (
        drugName text,
        recommendationId int,
        implications jsonb,
        drugRecommendation text,
        classification text,
        phenotypes jsonb,
        activityScore jsonb,
        alleleStatus jsonb,
        population text,
        comments text
    ) as
$$
select
    d.name as drugname,
    r.id as recommendationid,
    r.implications,
    r.drugrecommendation,
    r.classification,
    r.phenotypes,
    r.activityscore,
    r.allelestatus,
    r.population,
    r.comments
from recommendation r join drug d on r.drugid = d.drugid join guideline g on r.guidelineid = g.id
where lookupkey <@ (
        select jsonb_object_agg(key, value) as lookupkey
        from (select lookupkey from diplotype where diplotypekey <@ lookupkeyArg::jsonb) x, json_each(x.lookupkey)
    )
    and guidelineidArg = any(g.pharmgkbid)
$$ language SQL stable;

comment on function cpic.pharmgkb_guideline_recommendation(guidelineidArg text, lookupkeyArg text)
    is 'For a given PharmGKB guideline and genotype lookup JSON key, give the matching recommendations';
