create view guideline_summary_view as
(
select g.name                                                                   as guideline_name,
       g.url                                                                    as guideline_url,
       array_agg(distinct d.name)                                               as drugs,
       jsonb_agg(distinct jsonb_build_object('symbol', e.symbol, 'url', e.url)) as genes
from guideline g
         join drug d on (d.guidelineid = g.id),
     unnest(g.genes) gn
         join gene e on (e.symbol = gn)
group by g.name, g.url
);

comment on view guideline_summary_view is 'This view lists guidelines and the genes and drugs that are associated with them';
comment on column guideline_summary_view.guideline_name is 'The title of the guideline';
comment on column guideline_summary_view.guideline_url is 'The URL of the guideline on the CPIC website';
comment on column guideline_summary_view.drugs is 'An array of drug names associated with this guideline';
comment on column guideline_summary_view.genes is 'An array of gene data associated with this guideline, each object will have a symbol and a URL for the gene on the CPIC website';
