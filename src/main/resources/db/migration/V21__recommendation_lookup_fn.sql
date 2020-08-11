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
