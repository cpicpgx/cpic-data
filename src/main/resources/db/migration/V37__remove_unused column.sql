-- remove recommendation_view dependency
drop function recommendation_lookup;
-- remove recommendation.prescribingchange dependency
drop view recommendation_view;

-- recreate the recommendation_view without prescribing change
create view recommendation_view as
select
    r.id recommendationid,
    r.lookupKey,
    d.name drugname,
    g.name guidelinename,
    g.url guidelineurl,
    r.implications,
    r.drugrecommendation,
    r.classification,
    r.phenotypes,
    r.activityscore,
    r.population,
    r.comments
from recommendation r
         join drug d on r.drugid = d.drugid
         join guideline g on r.guidelineid = g.id;
comment on view recommendation_view is 'A view to help find recommendation data when querying by the lookupKey';

-- recreate the recommendation_lookup function
create or replace function cpic.recommendation_lookup(diplotypelookup text)
    returns setof cpic.recommendation_view as
$$
select * from recommendation_view where lookupkey <@ (
    select jsonb_object_agg(key, value) as lookupkey
    from (select lookupkey from diplotype where diplotypekey <@ diplotypelookup::jsonb) x,
        json_each(x.lookupkey)
)
$$ language SQL stable;
comment on function recommendation_lookup(diplotypelookup text) is 'This function helps translate diplotypes into recommendation data. This relies on the recommendation_view to gather and filter relevant information. The diplotypelookup parameter is a JSON object of gene symbol to JSON allele representation.';

-- drop the prescribingChange column from the recommendation table
alter table recommendation drop column prescribingChange;
