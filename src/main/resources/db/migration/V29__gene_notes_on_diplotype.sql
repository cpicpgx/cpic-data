alter table gene add column notes_on_diplotype text;
comment on column gene.notes_on_diplotype is 'Optional notes about the diplotypes of the gene';

update gene
set notes_on_diplotype='The DPYD phenotype is assigned using a gene activity score (DPYD-AS; based on the DPYD ' ||
                       'Allele Functionality Table), calculated as the sum of the two DPYD variants with the lowest ' ||
                       'variant activity value. Briefly, carriers of two no function variants (DPYD-AS: 0) OR one ' ||
                       'decreased function variant (DPD-AS: 0.5) are classified as DPYD poor metabolizers; carriers ' ||
                       'of two decreased function variants OR carriers of only one no function variant (DPYD-AS: 1) ' ||
                       'OR carriers of only one decreased function variant (DPYD-AS: 1.5) are considered DPYD ' ||
                       'intermediate metabolizers , and those with only normal function variants are classified as ' ||
                       'DPYD normal metabolizers (DPYD-AS: 2). If two decreased/no function variants are present, ' ||
                       'each decreased/no function variant is considered to be on a different gene copy. ' ||
                       'Irrespective of the presence of decreased/no function variants, patients may carry multiple ' ||
                       'normal function variants. As an individual only carries a maximum of two fully functional ' ||
                       'DPYD copies, common normal function variants may be located on the same gene copy as other ' ||
                       'normal function variants or decreased/no function variants.'
where symbol='DPYD';
