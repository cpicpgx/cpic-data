-- noinspection SqlDialectInspectionForFile
SET ROLE cpic;

CREATE OR REPLACE VIEW diplotype AS
select
    gp.genesymbol,
    d.diplotype,
    pf.function1,
    pf.function2,
    pf.activityvalue1,
    pf.activityvalue2,
    pf.totalactivityscore,
    pf.description,
    d.diplotypekey,
    gp.phenotype,
    gp.ehrPriority,
    gp.consultationText,
    json_build_object(gp.genesymbol, case when g.lookupMethod = 'ACTIVITY_SCORE' then pf.totalActivityScore else gp.phenotype end) as lookupkey
from
    phenotype_diplotype d
        join phenotype_function pf on d.functionphenotypeid = pf.id
        join gene_phenotype gp on pf.phenotypeid = gp.id
        join gene g on gp.geneSymbol = g.symbol;

COMMENT ON VIEW diplotype IS 'A combination of gene_phenotype and phenotype_diplotype that allows you to easily query by diplotype and see the phenotype-related data for it';
COMMENT ON COLUMN diplotype.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN diplotype.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';
COMMENT ON COLUMN diplotype.function1 IS 'A functional assignment of one of the alleles, optional';
COMMENT ON COLUMN diplotype.function2 IS 'A functional assignment of one of the alleles, optional';
COMMENT ON COLUMN diplotype.activityvalue1 IS 'An activity value assignment of one of the alleles, optional';
COMMENT ON COLUMN diplotype.activityvalue2 IS 'An activity value assignment of one of the alleles, optional';
COMMENT ON COLUMN diplotype.totalactivityscore IS 'The total Activity Score number, optional';
COMMENT ON COLUMN diplotype.description IS 'The long-form description of the diplotype';
COMMENT ON COLUMN diplotype.diplotypekey IS 'A normalized version of the diplotype that can be used for DB lookups';
COMMENT ON COLUMN diplotype.phenotype IS 'Coded Genotype/Phenotype Summary, required';
COMMENT ON COLUMN diplotype.lookupkey IS 'A normalized version of the gene phenotype that can be used for recommendation or test alert lookup';


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

COMMENT ON VIEW population_frequency_view IS 'A summary of frequency data by allele and major population group';
COMMENT ON COLUMN population_frequency_view.geneSymbol IS 'The HGNC symbol of the gene';
COMMENT ON COLUMN population_frequency_view.name IS 'The allele name';
COMMENT ON COLUMN population_frequency_view.population_group IS 'The major grouping of population';
COMMENT ON COLUMN population_frequency_view.subjectcount IS 'The count of subjects assigned the population group for the given allele';
COMMENT ON COLUMN population_frequency_view.freq_weighted_avg IS 'The average frequency weighted by total subjects';
COMMENT ON COLUMN population_frequency_view.freq_avg IS 'The unweighted average frequency (use with caution)';
COMMENT ON COLUMN population_frequency_view.freq_max IS 'The maximum frequency observed';
COMMENT ON COLUMN population_frequency_view.freq_min IS 'The minimum frequency observed';


create or replace view recommendation_view as
select
    r.id recommendationid,
    r.lookupKey,
    d.name drugname,
    g.name guidelinename,
    g.url guidelineurl,
    r.implications,
    r.drugrecommendation,
    r.classification,
    r.phenotypes,
    r.activityscore,
    r.population,
    r.comments,
    r.prescribingChange
from recommendation r
         join drug d on r.drugid = d.drugid
         join guideline g on r.guidelineid = g.id;

comment on view recommendation_view is 'A view to help find recommendation data when querying by the lookupKey';


create or replace view test_alert_view as
select
    t.id testalertid,
    t.lookupKey,
    d.name drugname,
    t.population,
    t.alertText,
    t.cdsContext
from test_alert t
         join drug d on t.drugId = d.drugId;

comment on view test_alert_view is 'A view to help find test alert data when querying by the lookupKey';
