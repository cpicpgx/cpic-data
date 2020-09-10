update drug set guidelineid=(select id from guideline where name='CYP2C9, HLA-B and Phenytoin') where drugid=(select drugid from drug where name='fosphenytoin');

COPY publication (title, authors, journal, month, page, volume, year, pmid) FROM stdin;
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for CYP2C9 and HLA-B Genotypes and Phenytoin Dosing: 2020 Update.	{"Karnes Jason H","Rettie Allan E","Somogyi Andrew A","Huddart Rachel","Fohner Alison E","Formea Christine M","Lee Ming Ta Michael","Llerena Adrian","Whirl-Carrillo Michelle","Klein Teri E","Phillips Elizabeth J","Mintzer Scott","Gaedigk Andrea","Caudle Kelly E","Callaghan John T"}	Clinical pharmacology and therapeutics	8			2020	32779747
\.

update publication set guidelineid=(select id from guideline where name='CYP2C9, HLA-B and Phenytoin') where pmid='32779747';