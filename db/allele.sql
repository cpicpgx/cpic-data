CREATE TABLE allele
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  hgncId VARCHAR(50) REFERENCES gene(hgncid) NOT NULL,
  name VARCHAR(200) NOT NULL,
  functionalStatus VARCHAR(200)
);

CREATE TABLE sequence_location
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  name VARCHAR(200),
  chromosomeLocation VARCHAR(200),
  geneLocation VARCHAR(200),
  proteinLocation VARCHAR(200),
  hgncId VARCHAR(50) REFERENCES gene(hgncId) NOT NULL,
  dbSnpId VARCHAR(20)
);

CREATE TABLE allele_location_value
(
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  locationid INTEGER NOT NULL REFERENCES sequence_location(id),
  variantAllele VARCHAR(200) NOT NULL
);

CREATE TABLE translation_note
(
  hgncId VARCHAR(50) REFERENCES gene(hgncId) NOT NULL,
  note TEXT NOT NULL
);

CREATE TABLE population
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  citation VARCHAR(200),
  ethnicity VARCHAR(50) NOT NULL,
  population VARCHAR(200),
  populationInfo VARCHAR(500),
  subjectType VARCHAR(500),
  subjectCount INTEGER DEFAULT 0
);
  
CREATE TABLE allele_frequency
(
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  population INTEGER NOT NULL REFERENCES population(id),
  frequency NUMERIC,
  label VARCHAR(50),
  
  UNIQUE (alleleid, population)
);

CREATE TABLE function_reference
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  pmid VARCHAR(50),
  finding TEXT,
  substrate_in_vitro TEXT[],
  substrate_in_vivo TEXT[]
);
