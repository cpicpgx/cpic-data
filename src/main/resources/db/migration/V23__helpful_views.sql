CREATE VIEW pair_view AS
select
    p.pairid,
    p.drugid,
    d.name as drugname,
    p.genesymbol,
    g.name as guidelinename,
    g.url as guidelineurl,
    p.cpiclevel,
    p.pgkbcalevel,
    p.pgxtesting,
    p.citations as pmids,
    case
        when (p.usedforrecommendation and p.guidelineid is not null) then 'Yes'
        when (not p.usedforrecommendation and p.guidelineid is not null) then 'No'
        else 'n/a' end usedForRecommendation
from pair p
         join drug d on p.drugid = d.drugid
         left join guideline g on d.guidelineid = g.id;
COMMENT ON VIEW pair_view IS 'This pairs view combines information from the pair, drug, and guideline tables to make a more readable view of pair data';
COMMENT ON COLUMN  pair_view.pairid IS 'The primary key ID of this pair';
COMMENT ON COLUMN  pair_view.drugid IS 'The ID of the drug in the pair';
COMMENT ON COLUMN  pair_view.drugname IS 'The name of the drug in the pair';
COMMENT ON COLUMN  pair_view.genesymbol IS 'The symbol of the drug in the pair';
COMMENT ON COLUMN  pair_view.guidelinename IS 'The name for the guideline of this pair';
COMMENT ON COLUMN  pair_view.guidelineurl IS 'The URL for the guideline of this pair';
COMMENT ON COLUMN  pair_view.cpiclevel IS 'The CPIC-assigned level of the pair';
COMMENT ON COLUMN  pair_view.pgkbcalevel IS 'The top level of PharmGKB Clinical Annotation of the pair';
COMMENT ON COLUMN  pair_view.pgxtesting IS 'The testing level of the label annotation from PharmGKB for this pair';
COMMENT ON COLUMN  pair_view.pmids IS 'The PMIDs for guideline publications of this pair';


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
