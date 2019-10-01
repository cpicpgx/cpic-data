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


CREATE VIEW population_frequency_view AS
select
    a.genesymbol, a.name,
    p.ethnicity as population_group,
    sum(subjectcount) as subjectcount,
    round((sum((subjectcount * frequency)/100) / sum(subjectcount))*100, 2) as freq_weighted_avg,
    round(avg(frequency), 2) as freq_avg
from
    population p
        join allele_frequency af on p.id = af.population
        join allele a on af.alleleid = a.id
where
    frequency is not null
group by
    a.genesymbol, a.name, p.ethnicity;
