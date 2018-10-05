set role cpic;

CREATE SEQUENCE cpic_id START 100000;

-- each inline script below contains the data model and the data for that entity

BEGIN;
\i ./guideline.sql
\i ./gene.sql
\i ./allele.sql
\i ./publication.sql
\i ./drug.sql
\i ./pair.sql
\i ./terms.sql
\i ./diplotype.sql
COMMIT;
