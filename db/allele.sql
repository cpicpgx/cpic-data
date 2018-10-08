CREATE TABLE allele
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  hgncId VARCHAR(50) REFERENCES gene(hgncid) NOT NULL,
  name VARCHAR(200) NOT NULL,
  functionalStatus VARCHAR(200)
);

COMMENT ON TABLE allele IS 'An allele of a gene';
COMMENT ON COLUMN allele.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN allele.hgncId IS 'The HGNC symbol of the gene the allele is for, required';
COMMENT ON COLUMN allele.name IS 'The name of this allele, required';
COMMENT ON COLUMN allele.functionalStatus IS 'The functional phenotype of this allele';


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

COMMENT ON TABLE sequence_location IS 'A location on a sequence';
COMMENT ON COLUMN sequence_location.name IS 'The short name of this sequence location, arbitrary but often the gene location and nucleotide change';
COMMENT ON COLUMN sequence_location.chromosomeLocation IS 'The partial HGVS representation of the location on the chromosomal sequence';
COMMENT ON COLUMN sequence_location.geneLocation IS 'The partial HGVS representation of the location on the gene sequence';
COMMENT ON COLUMN sequence_location.proteinLocation IS 'The partial HGVS representation of the location on the protein sequence';
COMMENT ON COLUMN sequence_location.hgncId IS 'The HGNC symbol fo the gene this squence location falls in';
COMMENT ON COLUMN sequence_location.dbSnpId IS 'The DBSNP identifier (rs#) for this location, optional';


CREATE TABLE allele_location_value
(
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  locationid INTEGER NOT NULL REFERENCES sequence_location(id),
  variantAllele VARCHAR(200) NOT NULL
);

COMMENT ON TABLE allele_location_value IS 'The change at a specific location for a specific allele';


CREATE TABLE translation_note
(
  hgncId VARCHAR(50) REFERENCES gene(hgncId) NOT NULL,
  note TEXT NOT NULL
);

COMMENT ON TABLE translation_note IS 'A note about allele translation for a gene';
COMMENT ON COLUMN translation_note.hgncId IS 'The HGNC gene symbol for the gene this note is about, required';
COMMENT ON COLUMN translation_note.note IS 'The text of the note about allele translation, required';


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
