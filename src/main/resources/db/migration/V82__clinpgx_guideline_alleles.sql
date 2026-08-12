------------------------------------------------------------------------------------------------------------------------
-- fix the queries in these functions and rename the parameters
------------------------------------------------------------------------------------------------------------------------

drop function cpic.clinpgx_guideline_alleles(text);

create function cpic.clinpgx_guideline_alleles(clinpgxdrugid text)
    returns TABLE(genesymbol text, chr text, lookupmethod text, alleles text[])
    stable
    language sql
as
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
    where d.clinpgxid= clinpgxdrugid
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
  and d.clinpgxid= clinpgxdrugid
  and upper(a.value) != 'NO RESULT'
group by g.symbol, g.chr, g.lookupmethod
$$;

comment on function cpic.clinpgx_guideline_alleles(text) is 'For a given ClinPGx drug ID, list the alleles that are options for looking up recommendations';

alter function cpic.clinpgx_guideline_alleles(text) owner to cpic;

grant execute on function cpic.clinpgx_guideline_alleles(text) to web_anon;


------------------------------------------------------------------------------------------------------------------------

drop function cpic.clinpgx_guideline_recommendation(text, text);

create or replace function cpic.clinpgx_guideline_recommendation(clinpgxdrugid text, lookupkeyarg text)
    returns TABLE(drugname text, recommendationid integer, implications jsonb, drugrecommendation text, classification text, phenotypes jsonb, activityscore jsonb, allelestatus jsonb, population text, comments text)
    stable
    language sql
as
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
  and d.clinpgxid=clinpgxdrugid
$$;

comment on function cpic.clinpgx_guideline_recommendation(text, text) is 'For a given ClinPGx guideline and genotype lookup JSON key, give the matching recommendations';

alter function cpic.clinpgx_guideline_recommendation(text, text) owner to cpic;

grant execute on function cpic.clinpgx_guideline_recommendation(text, text) to web_anon;
