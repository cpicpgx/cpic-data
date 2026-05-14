alter table pair drop CONSTRAINT valid_cpiclevel_check;
alter table pair add CONSTRAINT valid_cpiclevel_check CHECK (cpiclevel in ('A', 'A/B', 'B', 'B/C', 'C', 'C/D', 'D', 'Retired'));

drop view pair_view;

alter table pair alter column cpiclevel type text using cpiclevel::text;

create or replace view cpic.pair_view
            (pairid, drugid, drugname, genesymbol, guidelinename, guidelineurl, cpiclevel, clinpgxlevel, pgxtesting,
             pmids, usedforrecommendation, provisional)
as
SELECT p.pairid,
       p.drugid,
       d.name      AS drugname,
       p.genesymbol,
       g.name      AS guidelinename,
       g.url       AS guidelineurl,
       p.cpiclevel,
       p.clinpgxlevel,
       p.pgxtesting,
       p.citations AS pmids,
       CASE
           WHEN p.usedforrecommendation AND p.guidelineid IS NOT NULL THEN 'Yes'::text
           WHEN NOT p.usedforrecommendation AND p.guidelineid IS NOT NULL THEN 'No'::text
           ELSE 'n/a'::text
           END     AS usedforrecommendation,
       CASE
           WHEN p.guidelineid IS NULL THEN true
           ELSE false
           END     AS provisional
FROM pair p
         JOIN drug d ON p.drugid::text = d.drugid::text
         LEFT JOIN guideline g ON p.guidelineid = g.id
WHERE p.removed IS FALSE;

comment on view cpic.pair_view is 'This pairs view combines information from the pair, drug, and guideline tables to make a more readable view of pair data. This also limits result to pairs which have NOT been removed.';
comment on column cpic.pair_view.pairid is 'The primary key ID of this pair';
comment on column cpic.pair_view.drugid is 'The ID of the drug in the pair';
comment on column cpic.pair_view.drugname is 'The name of the drug in the pair';
comment on column cpic.pair_view.genesymbol is 'The symbol of the drug in the pair';
comment on column cpic.pair_view.guidelinename is 'The name for the guideline of this pair';
comment on column cpic.pair_view.guidelineurl is 'The URL for the guideline of this pair';
comment on column cpic.pair_view.cpiclevel is 'The CPIC-assigned level of the pair';
comment on column cpic.pair_view.clinpgxlevel is 'The top level of ClinPGx Summary Annotation of the pair';
comment on column cpic.pair_view.pgxtesting is 'The testing level of the label annotation from ClinPGx for this pair';
comment on column cpic.pair_view.pmids is 'The PMIDs for guideline publications of this pair';
comment on column cpic.pair_view.provisional is 'The CPIC level assigned to this pair is provisional, true if this pair is not part of a guideline';

alter table cpic.pair_view owner to cpic;
