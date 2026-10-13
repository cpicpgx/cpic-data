alter table allele_frequency add column subjectsobserved integer;
comment on column allele_frequency.subjectsobserved is 'The number of alleles this allele was observed on in the population, i.e. the numerator of the frequency';
