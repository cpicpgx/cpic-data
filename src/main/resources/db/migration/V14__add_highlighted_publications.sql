alter table publication add highlightedOnSite boolean default false;
COMMENT ON COLUMN publication.highlightedOnSite IS 'true means this publication will show in the CPIC site Publications page, defaults to false';

update publication set highlightedOnSite=true where pmid in ('21270786', '24479687', '27441996', '27026620', '27864205');
