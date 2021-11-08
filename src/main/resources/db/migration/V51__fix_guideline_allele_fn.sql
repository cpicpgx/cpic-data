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
    a.clinicalfunctionalstatus is not null
    and g.lookupmethod!='ALLELE_STATUS'
    and a.genesymbol in (
        select distinct jsonb_object_keys(p.lookupkey) as genes
        from recommendation p join guideline g on p.guidelineid = g.id join drug d on p.drugid=d.drugid
        where d.pharmgkbid=pharmgkbdrugid
    )
group by a.genesymbol, g.chr, g.lookupmethod
union all
select
    g.symbol as genesymbol,
    g.chr,
    g.lookupmethod,
    array_agg(distinct a.value order by a.value) as alleles
from
    recommendation r
    join drug d on r.drugid = d.drugid,
    jsonb_each_text(r.lookupkey) a
    join gene g on (a.key=g.symbol)
where
    r.allelestatus is not null
    and g.lookupmethod='ALLELE_STATUS'
    and d.pharmgkbid=pharmgkbdrugid
    and upper(a.value) != 'NO RESULT'
group by g.symbol, g.chr, g.lookupmethod
$$ language SQL stable;

comment on function cpic.pharmgkb_guideline_alleles(pharmgkbdrugid text)
    is 'For a given PharmGKB drug ID, list the alleles that are options for looking up recommendations';


drop function cpic.pharmgkb_guideline_recommendation;

create function cpic.pharmgkb_guideline_recommendation(drugidArg text, lookupkeyArg text) returns
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
    select jsonb_object_agg(a.key, coalesce(d.lookupkey -> a.key, a.value)) as lookup
    from
        json_each(lookupkeyArg::json) a
            left join diplotype d on (d.diplotypekey=jsonb_build_object(a.key, a.value))
)
  and d.pharmgkbid=drugidArg
$$ language SQL stable;

comment on function cpic.pharmgkb_guideline_recommendation(guidelineidArg text, lookupkeyArg text)
    is 'For a given PharmGKB guideline and genotype lookup JSON key, give the matching recommendations';
