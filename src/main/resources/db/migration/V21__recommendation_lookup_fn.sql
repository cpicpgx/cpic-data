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


create or replace function cpic.test_alert_lookup(lookup text)
    returns setof cpic.test_alert_view as
$$
select * from test_alert_view where lookupkey <@ (
    select jsonb_object_agg(key, value) as lookupkey
    from (select lookupkey from diplotype where diplotypekey <@ lookup::jsonb) x,
        json_each(x.lookupkey)
)
$$ language SQL stable;

comment on function test_alert_lookup(lookup text) is 'This function helps translate diplotypes into test alert data. This relies on the test_alert_view to gather and filter relevant information. The lookup parameter is a JSON object of gene symbol to JSON allele representation.';
