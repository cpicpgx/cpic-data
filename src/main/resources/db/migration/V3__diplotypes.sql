CREATE VIEW diplotype_view AS
  select
         p.genesymbol,
         d.diplotype,
         p.phenotype,
         p.ehrpriority,
         p.consultationtext,
         p.activityscore
  from
       gene_phenotype p
         join phenotype_diplotype d on p.id = d.phenotypeid;

COMMENT ON VIEW diplotype_view IS 'A combination of gene_phenotype and phenotype_diplotype that allows you to easily query by diplotype and see the phenotype-related data for it';
COMMENT ON COLUMN diplotype_view.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN diplotype_view.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';
COMMENT ON COLUMN diplotype_view.phenotype IS 'Coded Genotype/Phenotype Summary, required';
COMMENT ON COLUMN diplotype_view.ehrPriority IS 'EHR Priority Result, optional';
COMMENT ON COLUMN diplotype_view.consultationText IS 'Consultation (Interpretation) Text Provided with Test Result, optional';
COMMENT ON COLUMN diplotype_view.activityScore IS 'The Activity Score number, optional';
