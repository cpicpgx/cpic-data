do $$
    DECLARE
        guidelineName TEXT := 'SLCO1B1, ABCG2, CYP2C9, and Statins';
        guidelineUrl  TEXT := 'https://cpicpgx.org/guidelines/cpic-guideline-for-statins/';
        statinGuidelineId INT  := -1;
    BEGIN
        -- update guideline info to supercede the previous simvastatin guideline
        update guideline set name=guidelineName, genes='{SLCO1B1,ABCG2,CYP2C9}', url=guidelineUrl, pharmgkbId='{PA166105005,PA166262221,PA166262241,PA166262261,PA166262281,PA166262321,PA166262341}'
        where name='SLCO1B1 and Simvastatin' returning id into statinGuidelineId;

        update publication set guidelineid=statinGuidelineId where pmid='35152405';

        -- 1-gene pairs
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='simvastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='pravastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='pitavastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='atorvastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='lovastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);

        -- 2-gene pair for fluvastatin
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='fluvastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('CYP2C9', 'A', (select drugid from drug where name='fluvastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);

        -- 2-gene pair for rosuvastatin
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('SLCO1B1', 'A', (select drugid from drug where name='rosuvastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);
        insert into pair as p (genesymbol, cpiclevel, drugid, guidelineid, usedforrecommendation, citations) values ('ABCG2', 'A', (select drugid from drug where name='rosuvastatin'), (select id from guideline where name=guidelineName), true, '{35152405}') on conflict (genesymbol, drugid) do update set cpiclevel=excluded.cpiclevel, guidelineid=excluded.guidelineid, usedforrecommendation=excluded.usedforrecommendation, citations=array_cat(p.citations, excluded.citations);

        -- update drugs
        update drug set guidelineid=(select id from guideline where name=guidelineName), flowchart='https://files.cpicpgx.org/images/flow_chart/'||initcap(name)||'_CDS_Flow_Chart.jpg'
        where name in (
                       'simvastatin', 'rosuvastatin', 'pravastatin', 'pitavastatin', 'atorvastatin', 'fluvastatin', 'lovastatin'
            );
    END $$
