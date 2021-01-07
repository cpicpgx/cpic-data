-- ensure all drugs for the guideline exist, there are 5 new drugs
insert into drug(drugid, pharmgkbid, rxnormid, drugbankid, atcid, name) values ('RxNorm:1819', 'PA448685', '1819', 'DB00921', '{N02AE01,N07BC01}', 'buprenorphine') on conflict do nothing;
insert into drug(drugid, pharmgkbid, rxnormid, drugbankid, atcid, name) values ('RxNorm:3423', 'PA449918', '3423', 'DB00327', '{N02AA03}', 'hydromorphone') on conflict do nothing;
insert into drug(drugid, pharmgkbid, rxnormid, drugbankid, atcid, name) values ('RxNorm:236913', 'PA166227621', '236913', 'DB13515', '{N07BC05}', 'levomethadone') on conflict do nothing;
insert into drug(drugid, pharmgkbid, rxnormid, drugbankid, atcid, name) values ('RxNorm:73032', 'PA451232', '73032', 'DB00899', '{N01AH06}', 'remifentanil') on conflict do nothing;
insert into drug(drugid, pharmgkbid, rxnormid, drugbankid, atcid, name) values ('RxNorm:56795', 'PA451527', '56795', 'DB00708', '{N01AH03}', 'sufentanil') on conflict do nothing;

-- ensure all genes for the guideline exist, there are 2 new genes
insert into gene(symbol, chr, hgncid, ncbiid, ensemblid, pharmgkbid) values ('COMT', 'chr22', 'HGNC:2228', '1312', 'ENSG00000093010', 'PA117') on conflict do nothing;
insert into gene(symbol, chr, hgncid, ncbiid, ensemblid, pharmgkbid) values ('OPRM1', 'chr6', 'HGNC:8156', '4988', 'ENSG00000112038', 'PA31945') on conflict do nothing;

-- update the existing guideline to the new title and to include the additional genes
update guideline set name='CYP2D6, OPRM1, COMT, and Opioids', genes='{CYP2D6,COMT,OPRM1}' where name='CYP2D6 and Codeine';
-- link the guideline's publication to the guideline record
update publication set guidelineid=(select id from guideline where name='CYP2D6, OPRM1, COMT, and Opioids') where pmid='33387367';
-- link the drugs in the guideline to the guideline record
update drug set guidelineid=(select id from guideline where name='CYP2D6, OPRM1, COMT, and Opioids') where name in ('morphine', 'fentanyl', 'alfentanil', 'buprenorphine', 'codeine', 'hydrocodone', 'hydromorphone', 'levomethadone', 'methadone', 'naltrexone', 'oxycodone', 'remifentanil', 'sufentanil', 'tramadol');
