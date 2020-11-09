alter table publication add constraint publication_pmid unique (pmid);
alter table publication add constraint publication_pmcid unique (pmcid);
alter table publication add constraint publication_doi unique (doi);
