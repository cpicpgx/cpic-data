alter table gene add includephenotypefrequencies bool not null default true;
comment on column gene.includephenotypefrequencies is 'if allele frequencies exist for this gene, whether to calculate and display phenotype-level frequencies. a true value does not imply existence frequency data';

alter table gene add includediplotypefrequencies bool not null default true;
comment on column gene.includediplotypefrequencies is 'if allele frequencies exist for this gene, whether to calculate and display diplotype-level frequencies. a true value does not imply existence frequency data';

update gene set includephenotypefrequencies=false where array[symbol::text] <@ array['CACNA1S', 'CYP4F2', 'DPYD', 'G6PD', 'HLA-A', 'HLA-B', 'MT-RNR1', 'RYR1', 'UGT1A1', 'VKORC1'];
update gene set includediplotypefrequencies=false where array[symbol::text] <@ array['CACNA1S', 'CYP4F2', 'DPYD', 'G6PD', 'HLA-A', 'HLA-B', 'MT-RNR1', 'RYR1', 'UGT1A1', 'VKORC1'];
