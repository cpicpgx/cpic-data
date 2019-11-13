-- removing columns that have been replaced by the gene_note table
alter table gene drop column allelesLastModified;
alter table gene drop column functionalityreferencelastmodified;
