CREATE TABLE diplotype_phenotype
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  hgncId VARCHAR(50) REFERENCES gene(hgncid) NOT NULL,
  diplotype TEXT NOT NULL,
  phenotype TEXT,
  ehr TEXT,
  activityScore NUMERIC,
  
  UNIQUE (hgncId, diplotype)
);

COMMENT ON TABLE diplotype_phenotype IS 'A diplotype to phenotype translation';
COMMENT ON COLUMN diplotype_phenotype.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN diplotype_phenotype.hgncId IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN diplotype_phenotype.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';
COMMENT ON COLUMN diplotype_phenotype.phenotype IS 'Coded Genotype/Phenotype Summary, optional';
COMMENT ON COLUMN diplotype_phenotype.ehr IS 'EHR Priority Result, optional';
COMMENT ON COLUMN diplotype_phenotype.activityScore IS 'The Activity Score number, optional';
