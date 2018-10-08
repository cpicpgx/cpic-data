CREATE TABLE guideline
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  name VARCHAR(200) NOT NULL,
  url VARCHAR(200),
  pharmgkbId varchar(20)
);

COMMENT ON TABLE guideline IS 'A guideline for a drug or group of drugs';
COMMENT ON COLUMN guideline.id IS 'A synthetic numerical ID, primary key';
COMMENT ON COLUMN guideline.version IS 'The version number, iterates on modification';
COMMENT ON COLUMN guideline.name IS 'The name (title) of this guideline, required';
COMMENT ON COLUMN guideline.url IS 'The URL of this guideline on the cpicpgx.org domain, optional';
COMMENT ON COLUMN guideline.pharmgkbId IS 'The PharmGKB ID for this guideline, optional';

\copy guideline(name,url,pharmgkbId) from STDIN;
CPIC Guideline for peginterferon alfa-2a,peginterferon alfa-2b,ribavirin and IFNL3	https://cpicpgx.org/guidelines/guideline-for-peg-interferon-alpha-based-regimens-and-ifnl3/	PA166110235
CPIC Guideline for ivacaftor and CFTR	https://cpicpgx.org/guidelines/guideline-for-ivacaftor-and-cftr/	PA166114461
CPIC Guideline for azathioprine and TPMT	https://cpicpgx.org/guidelines/guideline-for-thiopurines-and-tpmt/	PA166104933
CPIC Guideline for rasburicase and G6PD	https://cpicpgx.org/guidelines/guideline-for-rasburicase-and-g6pd/	PA166119846
CPIC Guideline for mercaptopurine and TPMT	https://cpicpgx.org/guidelines/guideline-for-thiopurines-and-tpmt/	PA166104945
CPIC Guideline for clopidogrel and CYP2C19	https://cpicpgx.org/guidelines/guideline-for-clopidogrel-and-cyp2c19/	PA166104948
CPIC Guideline for warfarin and CYP2C9,CYP4F2,VKORC1	https://cpicpgx.org/guidelines/guideline-for-warfarin-and-cyp2c9-and-vkorc1/	PA166104949
CPIC Guideline for phenytoin and CYP2C9,HLA-B	https://cpicpgx.org/guidelines/guideline-for-phenytoin-and-cyp2c9-and-hla-b/	PA166122806
CPIC Guideline for thioguanine and TPMT	https://cpicpgx.org/guidelines/guideline-for-thiopurines-and-tpmt/	PA166104965
CPIC Guideline for codeine and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-codeine-and-cyp2d6/	PA166104996
CPIC Guideline for abacavir and HLA-B	https://cpicpgx.org/guidelines/guideline-for-abacavir-and-hla-b/	PA166104997
CPIC Guideline for nortriptyline and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166104998
CPIC Guideline for imipramine and CYP2C19,CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166104999
CPIC Guideline for doxepin and CYP2C19,CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166105000
CPIC Guideline for trimipramine and CYP2C19,CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166105001
CPIC Guideline for desipramine and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166105002
CPIC Guideline for allopurinol and HLA-B	https://cpicpgx.org/guidelines/guideline-for-allopurinol-and-hla-b/	PA166105003
CPIC Guideline for simvastatin and SLCO1B1	https://cpicpgx.org/guidelines/guideline-for-simvastatin-and-slco1b1/	PA166105005
CPIC Guideline for amitriptyline and CYP2C19,CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166105006
CPIC Guideline for clomipramine and CYP2C19,CYP2D6	https://cpicpgx.org/guidelines/guideline-for-tricyclic-antidepressants-and-cyp2d6-and-cyp2c19/	PA166105007
CPIC Guideline for carbamazepine and HLA-A,HLA-B	https://cpicpgx.org/guidelines/guideline-for-carbamazepine-and-hla-b/	PA166105008
CPIC Guideline for capecitabine and DPYD	https://cpicpgx.org/guidelines/guideline-for-fluoropyrimidines-and-dpyd/	PA166109594
CPIC Guideline for tropisetron and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-ondansetron-and-tropisetron-and-cyp2d6-genotype/	PA166161955
CPIC Guideline for tegafur and DPYD	https://cpicpgx.org/guidelines/guideline-for-fluoropyrimidines-and-dpyd/	PA166122687
CPIC Guideline for fluorouracil and DPYD	https://cpicpgx.org/guidelines/guideline-for-fluoropyrimidines-and-dpyd/	PA166122686
CPIC Guideline for voriconazole and CYP2C19	https://cpicpgx.org/guidelines/guideline-for-voriconazole-and-cyp2c19/	PA166161537
CPIC Guideline for oxcarbazepine and HLA-B	https://cpicpgx.org/guidelines/guideline-for-carbamazepine-and-hla-b/	PA166176623
CPIC Guideline for ondansetron and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-ondansetron-and-tropisetron-and-cyp2d6-genotype/	PA166161954
CPIC Guideline for tamoxifen and CYP2D6	https://cpicpgx.org/guidelines/cpic-guideline-for-tamoxifen-based-on-cyp2d6-genotype/	PA166176068
CPIC Guideline for tacrolimus and CYP3A5	https://cpicpgx.org/guidelines/guideline-for-tacrolimus-and-cyp3a5/	PA166124619
CPIC Guideline for paroxetine and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-selective-serotonin-reuptake-inhibitors-and-cyp2d6-and-cyp2c19/	PA166127636
CPIC Guideline for fluvoxamine and CYP2D6	https://cpicpgx.org/guidelines/guideline-for-selective-serotonin-reuptake-inhibitors-and-cyp2d6-and-cyp2c19/	PA166127637
CPIC Guideline for citalopram,escitalopram and CYP2C19	https://cpicpgx.org/guidelines/guideline-for-selective-serotonin-reuptake-inhibitors-and-cyp2d6-and-cyp2c19/	PA166127638
CPIC Guideline for sertraline and CYP2C19	https://cpicpgx.org/guidelines/guideline-for-selective-serotonin-reuptake-inhibitors-and-cyp2d6-and-cyp2c19/	PA166127639
CPIC Guideline for atazanavir and UGT1A1	https://cpicpgx.org/guidelines/guideline-for-atazanavir-and-ugt1a1/	PA166128738
\.
