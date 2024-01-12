create or replace function cpic.recommendation_lookup(diplotypelookup text)
    returns setof cpic.recommendation_view as
$$
select * from cpic.recommendation_view where lookupkey <@ (
    select jsonb_object_agg(key, value) as lookupkey
    from (select lookupkey from cpic.diplotype where diplotypekey <@ diplotypelookup::jsonb) x,
        json_each(x.lookupkey)
)
$$ language SQL stable security definer;
grant execute on function cpic.recommendation_lookup to web_anon;


create or replace function cpic.test_alert_lookup(lookup text)
    returns setof cpic.test_alert_view as
$$
select * from cpic.test_alert_view where lookupkey <@ (
    select jsonb_object_agg(key, value) as lookupkey
    from (select lookupkey from cpic.diplotype where diplotypekey <@ lookup::jsonb) x,
        json_each(x.lookupkey)
)
$$ language SQL stable security definer;
grant execute on function cpic.test_alert_lookup to web_anon;
