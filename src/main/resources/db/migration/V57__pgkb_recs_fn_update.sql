-- Dropping and re-creating this funciton so we can add the "guidelineurl" column, otherwise exactly the same

drop function cpic.pharmgkb_recommendation(lookupKeyArg text);

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
        comments text,
        guidelineurl text
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
    r.comments,
    g.url as guidelineurl
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
