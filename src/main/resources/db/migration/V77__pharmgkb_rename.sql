alter table cpic.guideline drop column pharmgkbid;

alter table cpic.gene rename column pharmgkbid to clinpgxid;
COMMENT ON COLUMN gene.clinpgxid IS 'The ID for this gene in ClinPGx';

alter table cpic.drug rename column pharmgkbid to clinpgxid;
COMMENT ON COLUMN drug.clinpgxid IS 'The ClinPGx ID for this drug, optional';

alter table guideline add unique (clinpgxid);