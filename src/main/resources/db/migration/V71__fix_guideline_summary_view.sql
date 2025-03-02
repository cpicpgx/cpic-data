create or replace view guideline_summary_view as
SELECT g.name                                                                   AS guideline_name,
       g.url                                                                    AS guideline_url,
       array_agg(DISTINCT d.name)                                               AS drugs,
       jsonb_agg(DISTINCT jsonb_build_object('symbol', p.genesymbol, 'url', 'https://cpicpgx.org/gene/'||lower(p.genesymbol)||'/')) AS genes
FROM guideline g
         join pair p on g.id = p.guidelineid
         join drug d on p.drugid=d.drugid
GROUP BY g.name, g.url;
