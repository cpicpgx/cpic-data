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
