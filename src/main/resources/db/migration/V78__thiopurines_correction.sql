insert into publication_supplement(publicationid, description, url)
select id, 'Correction to the 2026 thiopurines publication', 'https://files.cpicpgx.org/data/guideline/publication/thiopurines/2026/41618934-correction.pdf' from publication where pmid='41618934' on conflict do nothing;
