CREATE VIEW diplotype_view AS
  select
         p.genesymbol,
         d.diplotype,
         p.phenotype,
         p.ehrpriority,
         p.consultationtext,
         p.activityscore
  from
       gene_phenotype p
         join phenotype_diplotype d on p.id = d.phenotypeid;
