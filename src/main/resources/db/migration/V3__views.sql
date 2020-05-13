-- noinspection SqlDialectInspectionForFile
SET ROLE cpic;

CREATE OR REPLACE VIEW diplotype_view AS
select
    p.genesymbol,
    d.diplotype,
    p.phenotype,
    p.ehrpriority,
    p.consultationtext,
    f.totalActivityScore as activityscore,
    f.function1,
    f.function2
from
    gene_phenotype p
    join phenotype_function f on p.id = f.phenotypeId
    join phenotype_diplotype d on f.id = d.functionPhenotypeId;

COMMENT ON VIEW diplotype_view IS 'A combination of gene_phenotype and phenotype_diplotype that allows you to easily query by diplotype and see the phenotype-related data for it';
COMMENT ON COLUMN diplotype_view.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN diplotype_view.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';
COMMENT ON COLUMN diplotype_view.phenotype IS 'Coded Genotype/Phenotype Summary, required';
COMMENT ON COLUMN diplotype_view.ehrPriority IS 'EHR Priority Result, optional';
COMMENT ON COLUMN diplotype_view.consultationText IS 'Consultation (Interpretation) Text Provided with Test Result, optional';
COMMENT ON COLUMN diplotype_view.activityScore IS 'The Activity Score number, optional';
COMMENT ON COLUMN diplotype_view.function1 IS 'A functional assignment of one of the alleles, optional';
COMMENT ON COLUMN diplotype_view.function2 IS 'A functional assignment of one of the alleles, optional';


CREATE OR REPLACE VIEW allele_guideline_view AS
select distinct
    a.genesymbol,
    a.name as allele_name,
    g.name as guideline_name,
    g.url as guideline_url
from
    allele a
        join pair p on (a.genesymbol=p.genesymbol)
        join guideline g on (g.id=p.guidelineid)
where
    a.clinicalfunctionalstatus is not null;

COMMENT ON VIEW allele_guideline_view IS 'A combination of alleles and the guidelines they appear in';
COMMENT ON COLUMN allele_guideline_view.genesymbol IS 'The HGNC symbol of the gene';
COMMENT ON COLUMN allele_guideline_view.allele_name IS 'The name of the allele';
COMMENT ON COLUMN allele_guideline_view.guideline_name IS 'The name of the guideline this allele appears in';
COMMENT ON COLUMN allele_guideline_view.guideline_url IS 'The URL to the guideline';


CREATE OR REPLACE VIEW population_frequency_view AS
select
    a.genesymbol, a.name,
    p.ethnicity as population_group,
    sum(subjectcount) as subjectcount,
    (sum((subjectcount * frequency)/100) / sum(subjectcount))*100 as freq_weighted_avg,
    avg(frequency) as freq_avg,
    max(frequency) freq_max,
    min(frequency) freq_min
from
    population p
        join allele_frequency af on p.id = af.population
        join allele a on af.alleleid = a.id
where
    frequency is not null
group by
    a.genesymbol, a.name, p.ethnicity;

COMMENT ON VIEW population_frequency_view IS 'An summary of frequency data by allele and major population group';
COMMENT ON COLUMN population_frequency_view.geneSymbol IS 'The HGNC symbol of the gene';
COMMENT ON COLUMN population_frequency_view.name IS 'The allele name';
COMMENT ON COLUMN population_frequency_view.population_group IS 'The major grouping of population';
COMMENT ON COLUMN population_frequency_view.subjectcount IS 'The count of subjects assigned the population group for the given allele';
COMMENT ON COLUMN population_frequency_view.freq_weighted_avg IS 'The average frequency weighted by total subjects';
COMMENT ON COLUMN population_frequency_view.freq_avg IS 'The unweighted average frequency (use with caution)';
COMMENT ON COLUMN population_frequency_view.freq_max IS 'The maximum frequency observed';
COMMENT ON COLUMN population_frequency_view.freq_min IS 'The minimum frequency observed';
