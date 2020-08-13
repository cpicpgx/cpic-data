CREATE VIEW pair_view AS
select
    p.pairid,
    d.name as drugname,
    p.genesymbol,
    g.name as guidelinename,
    g.url as guidelineurl,
    p.level as cpiclevel,
    p.pgkbcalevel,
    p.pgxtesting,
    p.citations as pmids
from pair p
         join drug d on p.drugid = d.drugid
         left join guideline g on d.guidelineid = g.id;
COMMENT ON VIEW pair_view IS 'This pairs view combines information from the pair, drug, and guideline tables to make a more readable view of pair data';


CREATE VIEW change_log_view AS
with x as (
    select symbol as entityid, symbol as entityname from gene
    union all
    select drugid as entityid, name as entityname from drug
)
select
    l.date,
    l.type,
    l.entityid,
    x.entityname,
    l.note,
    l.version
from
    change_log l
        left join x on (l.entityid=x.entityid);
COMMENT ON VIEW change_log_view IS 'This view of the change_log table add the entity name to make it more readable';
