-- add the new PMID to the guideline
update publication set guidelineid=(select guidelineid from publication where pmid='28002639') where pmid='41979467';

-- add links to publication files
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/ondansetron/2026/41979467.pdf' where pmid='41979467';
insert into publication_supplement(publicationid, description, url)
    select id, 'Supplement to publication', 'https://files.cpicpgx.org/data/guideline/publication/ondansetron/2026/41979467-supplement.pdf' from publication where pmid='41979467';

-- insert ramosetron
insert into drug(drugid, name, clinpgxid, drugbankid) values ('Drugbank:DB09290', 'ramosetron', 'PA166365201', 'DB09290');

-- associate new drugs with the guideline
update drug set guidelineid=(select guidelineid from publication where pmid='28002639') where name in ('dolasetron', 'palonosetron', 'ramosetron');

-- add new PMID to existing pairs
update pair set citations=ARRAY['28002639', '41979467'] where citations @> ARRAY['28002639'];
-- update existing non-rec pairs to use new guideline
update pair
set
    guidelineid=(select guidelineid from publication where pmid='28002639'),
    usedforrecommendation=false,
    citations=ARRAY['41979467']
where drugid in (
    select drugid from drug where name in ('dolasetron', 'palonosetron')
);
-- insert new non-rec pair
insert into pair(genesymbol, drugid, guidelineid, usedforrecommendation, citations, cpiclevel)
select 'CYP2D6', drugid, guidelineid, false, ARRAY['41979467'], 'C' from drug where name='ramosetron';
