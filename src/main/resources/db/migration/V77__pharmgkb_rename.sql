-- rename references to PharmGKB to ClinPGx
alter table cpic.guideline drop column pharmgkbid;
alter table cpic.gene rename column pharmgkbid to clinpgxid;
COMMENT ON COLUMN gene.clinpgxid IS 'The ID for this gene in ClinPGx';
alter table cpic.drug rename column pharmgkbid to clinpgxid;
COMMENT ON COLUMN drug.clinpgxid IS 'The ClinPGx ID for this drug, optional';
alter table guideline add unique (clinpgxid);

-- remove "old" guideline entries that have been replaced by "new" guideline updates that are more inclusive
update publication set guidelineid=100413 where guidelineid=3070400;
update publication set guidelineid=2405438 where guidelineid=100420;
delete from guideline where id in (3070400, 100420);

-- change guideline links from cpicpgx.org to clinpgx.org
update guideline set url='https://www.clinpgx.org/guideline/'||clinpgxid where url ~ 'cpicpgx.org';
