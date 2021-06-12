insert into guideline(name, url, pharmgkbid, genes) values ('MT-RNR1 and Aminoglycosides', 'https://cpicpgx.org/guidelines/cpic-guideline-for-aminoglycosides-and-mt-rnr1/', '{PA166245501}', '{MT-RNR1}');

insert into pair(genesymbol, drugid, guidelineid, cpiclevel, citations) select 'MT-RNR1',d.drugid,g.id,'A','{34032273}' from drug d, guideline g where d.name='paromomycin' and g.name='MT-RNR1 and Aminoglycosides';
insert into pair(genesymbol, drugid, guidelineid, cpiclevel, citations) select 'MT-RNR1',d.drugid,g.id,'A','{34032273}' from drug d, guideline g where d.name='plazomicin' and g.name='MT-RNR1 and Aminoglycosides';

update pair set guidelineid=(select id from guideline where name='MT-RNR1 and Aminoglycosides'), usedforrecommendation=true, cpiclevel='A'
where drugid in (select drugid from drug where name in ('amikacin', 'gentamicin', 'kanamycin', 'paromomycin', 'plazomicin', 'streptomycin', 'tobramycin')) and genesymbol='MT-RNR1';

update drug set guidelineid=(select id from guideline where name='MT-RNR1 and Aminoglycosides')
where name in (
               'amikacin',
               'gentamicin',
               'kanamycin',
               'paromomycin',
               'plazomicin',
               'streptomycin',
               'tobramycin'
    );
