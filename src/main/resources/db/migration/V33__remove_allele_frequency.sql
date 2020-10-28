-- Removing this specific property since frequency data for alleles should be gathered from the allele_frequency table
-- instead of this property which was pulled from a different place
alter table allele drop column frequency;
