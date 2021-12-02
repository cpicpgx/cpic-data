drop view if exists recommendation_alleles_view;
create view recommendation_alleles_view
as (
   select a.genesymbol,
          g.chr,
          g.lookupmethod,
          array_agg(distinct a.name order by a.name) as alleles,
          count(distinct a.name)                     as allele_count
   from allele a
            join gene g on a.genesymbol = g.symbol
   where a.clinicalfunctionalstatus is not null
     and g.lookupmethod != 'ALLELE_STATUS'
   group by a.genesymbol, g.chr, g.lookupmethod
   union
   select g.symbol                                     as genesymbol,
          g.chr,
          g.lookupmethod,
          array_agg(distinct a.value order by a.value) as alleles,
          count(distinct a.value)                      as allele_count
   from recommendation r
            join drug d on r.drugid = d.drugid,
        jsonb_each_text(r.lookupkey) a
            join gene g on (a.key = g.symbol)
   where r.allelestatus is not null
     and g.lookupmethod = 'ALLELE_STATUS'
     and upper(a.value) not like '%NO%RESULT%'
   group by g.symbol, g.chr, g.lookupmethod
);

comment on view recommendation_alleles_view is 'A list of genes that are used for recommendation lookup and the alleles to use for those lookups';




create function cpic.pharmgkb_recommendation(lookupKeyArg text) returns
    table
    (
        drugName text,
        drugId text,
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
    d.pharmgkbid as drugid,
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
        json_each(lookupKeyArg::json) a
            left join diplotype d on (d.diplotypekey=jsonb_build_object(a.key, a.value))
)
$$ language SQL stable;

comment on function cpic.pharmgkb_recommendation(lookupkeyArg text)
    is 'For a given genotype lookup JSON key, give the matching recommendations for all available drugs';
