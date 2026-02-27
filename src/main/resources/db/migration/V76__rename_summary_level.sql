alter table pair rename column pgkbcalevel to clinpgxlevel;
alter view pair_view rename column pgkbcalevel to clinpgxlevel;

comment on column cpic.pair_view.clinpgxlevel is 'The top level of ClinPGx Summary Annotation of the pair';
comment on column cpic.pair_view.pgxtesting is 'The testing level of the label annotation from ClinPGx for this pair';
