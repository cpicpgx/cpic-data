SET ROLE cpic;

CREATE VIEW gene_drug_pair AS
select
    d.name as drugname,
    d.drugid,
    p.genesymbol as gene,
    p.level,
    p.pgkbcalevel,
    p.pgxtesting,
    p.citations,
    g.url as guidelineurl,
    p.pairid
from 
     pair p 
         join drug d on p.drugid = d.drugid 
         left join guideline g on p.guidelineid = g.id;

COMMENT ON VIEW gene_drug_pair IS 'A view of the pairs table with extra information needed for typical use';
COMMENT ON COLUMN gene_drug_pair.drugname IS 'The name of the drug in the pair';
COMMENT ON COLUMN gene_drug_pair.drugid IS 'The ID of the drug in the pair';
COMMENT ON COLUMN gene_drug_pair.gene IS 'The symbol of the drug in the pair';
COMMENT ON COLUMN gene_drug_pair.level IS 'The CPIC-assigned level of the pair';
COMMENT ON COLUMN gene_drug_pair.pgkbcalevel IS 'The top level of PharmGKB Clinical Annotation of the pair';
COMMENT ON COLUMN gene_drug_pair.pgxtesting IS 'The testing level of the label annotation from PharmGKB for this pair';
COMMENT ON COLUMN gene_drug_pair.citations IS 'The PMIDs for guideline publications of this pair';
COMMENT ON COLUMN gene_drug_pair.guidelineurl IS 'The URL for the guideline of this pair';
COMMENT ON COLUMN gene_drug_pair.pairid IS 'The primary key ID of this pair';
