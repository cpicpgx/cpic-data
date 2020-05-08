alter table drug add guidelineId INTEGER REFERENCES guideline(id);

COMMENT ON COLUMN drug.guidelineId IS 'The guideline this drug is part of a guideline, null means not a part of any guideline';

update drug d set guidelineId=(select distinct guidelineid from pair p where p.drugid=d.drugid and p.guidelineid is not null) where drugid in (select drugid from pair where guidelineid is not null);
