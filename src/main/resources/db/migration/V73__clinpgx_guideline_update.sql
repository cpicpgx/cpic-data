alter table guideline add clinpgxid text;
comment on column guideline.clinpgxid is 'The accession ID in ClinPGx for the guideline';

update guideline set clinpgxid='PA166251441' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-proton-pump-inhibitors-and-cyp2c19/';
update guideline set clinpgxid='PA166251442' where url='https://cpicpgx.org/guidelines/guideline-for-thiopurines-and-tpmt/';
update guideline set clinpgxid='PA166251443' where url='https://cpicpgx.org/guidelines/guideline-for-clopidogrel-and-cyp2c19/';
update guideline set clinpgxid='PA166251444' where url='https://cpicpgx.org/guidelines/guideline-for-abacavir-and-hla-b/';
update guideline set clinpgxid='PA166251445' where url='https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/';
update guideline set clinpgxid='PA166251446' where url='https://cpicpgx.org/guidelines/guideline-for-allopurinol-and-hla-b/';
update guideline set clinpgxid='PA166251448' where url='https://cpicpgx.org/guidelines/guideline-for-carbamazepine-and-hla-b/';
update guideline set clinpgxid='PA166251451' where url='https://cpicpgx.org/guidelines/guideline-for-tacrolimus-and-cyp3a5/';
update guideline set clinpgxid='PA166251453' where url='https://cpicpgx.org/guidelines/guideline-for-atazanavir-and-ugt1a1/';
update guideline set clinpgxid='PA166251454' where url='https://cpicpgx.org/guidelines/guideline-for-codeine-and-cyp2d6/';
update guideline set clinpgxid='PA166251455' where url='https://cpicpgx.org/guidelines/guideline-for-peg-interferon-alpha-based-regimens-and-ifnl3/';
update guideline set clinpgxid='PA166251456' where url='https://cpicpgx.org/guidelines/guideline-for-voriconazole-and-cyp2c19/';
update guideline set clinpgxid='PA166251457' where url='https://cpicpgx.org/guidelines/guideline-for-ondansetron-and-tropisetron-and-cyp2d6-genotype/';
update guideline set clinpgxid='PA166251458' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-tamoxifen-based-on-cyp2d6-genotype/';
update guideline set clinpgxid='PA166251459' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-atomoxetine-based-on-cyp2d6-genotype/';
update guideline set clinpgxid='PA166251460' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-ryr1-and-cacna1s/';
update guideline set clinpgxid='PA166251461' where url='https://cpicpgx.org/guidelines/guideline-for-phenytoin-and-cyp2c9-and-hla-b/';
update guideline set clinpgxid='PA166251462' where url='https://cpicpgx.org/guidelines/guideline-for-fluoropyrimidines-and-dpyd/';
update guideline set clinpgxid='PA166251463' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-efavirenz-based-on-cyp2b6-genotype/';
update guideline set clinpgxid='PA166251464' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-nsaids-based-on-cyp2c9-genotype/';
update guideline set clinpgxid='PA166251465' where url='https://cpicpgx.org/guidelines/guideline-for-warfarin-and-cyp2c9-and-vkorc1/';
update guideline set clinpgxid='PA166251466' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-aminoglycosides-and-mt-rnr1/';
update guideline set clinpgxid='PA166264281' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-statins/';
update guideline set clinpgxid='PA166343383' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-beta-blockers/';
update guideline set clinpgxid='PA166251450' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-g6pd/';
update guideline set clinpgxid='PA166411641' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-methadone-based-on-cyp2b6-genotype/';
update guideline set clinpgxid='PA166251449' where url='https://cpicpgx.org/guidelines/guideline-for-ivacaftor-and-cftr/';
update guideline set clinpgxid='PA166412761' where url='https://cpicpgx.org/guidelines/cpic-guideline-for-hydralazine-and-nat2/';

-- update the new SSRI guideline with all links
update guideline set clinpgxid='PA166251452', pharmgkbid='{PA166127638,PA166127637,PA166127636,PA166127639,PA166288221,PA166288601,PA166288581,PA166288582,PA166288561,PA166288541,PA166288201}'
                 where url='https://cpicpgx.org/guidelines/cpic-guideline-for-ssri-and-snri-antidepressants/';
-- remove existing links from old SSRI guideline
update guideline set pharmgkbid=null
                 where url='https://cpicpgx.org/guidelines/guideline-for-selective-serotonin-reuptake-inhibitors-and-cyp2d6-and-cyp2c19/';
