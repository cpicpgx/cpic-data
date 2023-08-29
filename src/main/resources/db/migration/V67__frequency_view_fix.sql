CREATE OR REPLACE VIEW population_frequency_view AS
select
    a.genesymbol, a.name,
    p.ethnicity as population_group,
    sum(subjectcount) as subjectcount,
    case
        when sum(subjectcount) > 0 then (sum((subjectcount * af.frequency)/100) / sum(subjectcount))*100
        else 0
        end freq_weighted_avg,
    avg(af.frequency) as freq_avg,
    max(af.frequency) freq_max,
    min(af.frequency) freq_min
from
    population p
        join allele_frequency af on p.id = af.population
        join allele a on af.alleleid = a.id
where
    af.frequency is not null
group by
    a.genesymbol, a.name, p.ethnicity;
