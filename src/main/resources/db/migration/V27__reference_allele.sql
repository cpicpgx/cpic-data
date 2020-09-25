ALTER TABLE allele_definition ADD reference BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN allele_definition.reference IS 'Indicates (with "true") if this allele is the "reference" allele which other alleles are compared against for their definition, defaults to false, should only be 1 per gene';

-- flag the existing reference alleles
update allele_definition set reference=true where genesymbol='CACNA1S' and name='Reference';
update allele_definition set reference=true where genesymbol='CFTR'    and name='ivacaftor non-responsive CFTR sequence';
update allele_definition set reference=true where genesymbol='CYP2B6'  and name='*1';
update allele_definition set reference=true where genesymbol='CYP2C19' and name='*1';
update allele_definition set reference=true where genesymbol='CYP2C9'  and name='*1';
update allele_definition set reference=true where genesymbol='CYP2D6'  and name='*1';
update allele_definition set reference=true where genesymbol='CYP3A5'  and name='*1';
update allele_definition set reference=true where genesymbol='CYP4F2'  and name='*1';
update allele_definition set reference=true where genesymbol='DPYD'    and name='Reference';
update allele_definition set reference=true where genesymbol='G6PD'    and name='B (wildtype)';
update allele_definition set reference=true where genesymbol='IFNL3'   and name='rs12979860 reference (C)';
update allele_definition set reference=true where genesymbol='NUDT15'  and name='*1';
update allele_definition set reference=true where genesymbol='RYR1'    and name='Reference';
update allele_definition set reference=true where genesymbol='SLCO1B1' and name='*1A';
update allele_definition set reference=true where genesymbol='TPMT'    and name='*1';
update allele_definition set reference=true where genesymbol='UGT1A1'  and name='*1';
update allele_definition set reference=true where genesymbol='VKORC1'  and name='rs9923231 reference (C)';
