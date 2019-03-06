CREATE VIEW allele_guideline_view AS
select distinct
  a.genesymbol,
  a.name as allele_name,
  g.name as guideline_name,
  g.url as guideline_url
from
  allele a
    join pair p on (a.genesymbol=p.genesymbol)
    join guideline g on (g.id=p.guidelineid);

COMMENT ON VIEW allele_guideline_view IS 'A combination of alleles and the guidelines they appear in';
COMMENT ON COLUMN allele_guideline_view.genesymbol IS 'The HGNC symbol of the gene';
COMMENT ON COLUMN allele_guideline_view.allele_name IS 'The name of the allele';
COMMENT ON COLUMN allele_guideline_view.guideline_name IS 'The name of the guideline this allele appears in';
COMMENT ON COLUMN allele_guideline_view.guideline_url IS 'The URL to the guideline';
