insert into guideline(name, url, pharmgkbid, genes) values ('CYP2C19 and Proton Pump Inhibitors', 'https://cpicpgx.org/guidelines/cpic-guideline-for-proton-pump-inhibitors-and-cyp2c19/', '{PA166219103, PA166219301}', '{CYP2C19}');

update pair set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:816346' and genesymbol='CYP2C19';
update pair set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:17128' and genesymbol='CYP2C19';
update pair set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:7646' and genesymbol='CYP2C19';
update pair set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:40790' and genesymbol='CYP2C19';

update drug set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:816346';
update drug set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:17128';
update drug set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:7646';
update drug set guidelineid=(select id from guideline where name='Proton Pump Inhibitors and CYP2C19') where drugid='RxNorm:40790';
