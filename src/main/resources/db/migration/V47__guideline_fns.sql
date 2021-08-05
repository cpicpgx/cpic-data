create function cpic.pharmgkb_guideline_alleles(guidelineid text)
    returns table(genesymbol text, chr text, alleles text[]) as
$$
select
   a.genesymbol,
   gene.chr,
   array_agg(a.name) as alleles
from guideline g join allele a on a.genesymbol = any(g.genes) join gene on gene.symbol=a.genesymbol
where guidelineid = any(g.pharmgkbid) group by a.genesymbol, gene.chr order by a.genesymbol
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
