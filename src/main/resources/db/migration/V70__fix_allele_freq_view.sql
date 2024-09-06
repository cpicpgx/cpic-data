CREATE OR REPLACE VIEW population_frequency_view AS
with x as (select a.id  as alleleid,
                  a.genesymbol,
                  a.name,
                  f.key as population_group,
                  case
                      when jsonb_typeof(f.value) = 'null' then null
                      else f.value::numeric
                      end  freq_weighted_avg
           from allele a
                    cross join lateral jsonb_each(a.frequency) f),
     y as (select f.alleleid,
                  p.ethnicity,
                  max(f.frequency)    as freq_max,
                  min(f.frequency)    as freq_min,
                  avg(f.frequency)    as freq_avg,
                  sum(p.subjectcount) as subjectcount
           from population p
                    join allele_frequency f on p.id = f.population
           group by f.alleleid, p.ethnicity)
select x.genesymbol,
       x.name,
       x.population_group,
       y.subjectcount,
       x.freq_weighted_avg,
       y.freq_avg,
       y.freq_max,
       y.freq_min
from x join y on x.alleleid = y.alleleid and x.population_group = y.ethnicity;
