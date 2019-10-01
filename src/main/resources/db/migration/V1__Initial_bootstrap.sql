SET ROLE cpic;

CREATE SEQUENCE cpic_id START 110000;

CREATE OR REPLACE FUNCTION increment_version()
  RETURNS TRIGGER
AS
$body$
BEGIN
  new.version := new.version + 1;
  return new;
END;
$body$
  LANGUAGE plpgsql;


CREATE TABLE guideline
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  name VARCHAR(200) UNIQUE NOT NULL,
  url VARCHAR(200) UNIQUE,
  pharmgkbId TEXT[]
);

CREATE TRIGGER version_guideline
  BEFORE UPDATE ON guideline
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE guideline IS 'A guideline for a drug or group of drugs';
COMMENT ON COLUMN guideline.id IS 'A synthetic numerical ID, primary key';
COMMENT ON COLUMN guideline.version IS 'The version number, iterates on modification';
COMMENT ON COLUMN guideline.name IS 'The name (title) of this guideline, required';
COMMENT ON COLUMN guideline.url IS 'The URL of this guideline on the cpicpgx.org domain, optional';
COMMENT ON COLUMN guideline.pharmgkbId IS 'The IDs from PharmGKB for their annotations of this guideline, optional';


CREATE TABLE publication
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  guidelineId INTEGER REFERENCES guideline(id),
  title VARCHAR(200) UNIQUE,
  authors TEXT[],
  journal VARCHAR(200),
  month INTEGER,
  page VARCHAR(50),
  volume VARCHAR(50),
  year INTEGER,
  pmid TEXT,
  pmcid TEXT,
  doi TEXT,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_publication
  BEFORE UPDATE ON publication
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE publication IS 'Published literature';
COMMENT ON COLUMN publication.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN publication.guidelineId IS 'The ID for the guideline this publication is about, optional';
COMMENT ON COLUMN publication.pmid IS 'The PubMed identifier for this publication';
COMMENT ON COLUMN publication.pmcid IS 'The PubMed Central identifier for this publication';
COMMENT ON COLUMN publication.doi IS 'The Document Object Identifier (DOI) for this publication';


CREATE TABLE gene
(
  symbol            VARCHAR(20) PRIMARY KEY NOT NULL,
  chr               VARCHAR(20) CHECK (chr ~ '^chr\S+$'),
  geneSequenceId    TEXT CHECK (geneSequenceId ~ '^(NG_\S+){0,1}$'),
  proteinSequenceId TEXT CHECK (proteinSequenceId ~ '^(NP_\S+){0,1}$'),
  chromoSequenceId  TEXT CHECK (chromoSequenceId ~ '^(NC_\S+){0,1}$'),
  hgncId            TEXT CHECK (hgncId ~ '^(HGNC:\d+){0,1}$'),
  ncbiId            TEXT,
  ensemblId         TEXT CHECK (ensemblId ~ '^(ENSG\d+){0,1}$'),
  pharmgkbId        TEXT CHECK (pharmgkbId ~ '^(PA\d+){0,1}$'),
  allelesLastModified                 DATE,
  functionalityReferenceLastModified  DATE,
  version           INTEGER DEFAULT 1
);

CREATE TRIGGER version_gene
  BEFORE UPDATE ON gene
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE gene IS 'Gene information with a primary key of the approved HGNC symbol for the gene. This means any gene used in the table must be approved by HGNC.';
COMMENT ON COLUMN gene.symbol IS 'Approved HGNC symbol, primary key.';
COMMENT ON COLUMN gene.chr IS 'Chromosome symbol. In the form chr##, where ## is the number or X/Y.';
COMMENT ON COLUMN gene.geneSequenceId IS 'The RefSeq ID for the sequence that represents this gene, starts with "NG_". No version suffix.';
COMMENT ON COLUMN gene.proteinSequenceId IS 'The RefSeq ID for the sequence that represents the protein product of this gene, starts with "NP_". No version suffix.';
COMMENT ON COLUMN gene.chromoSequenceId IS 'The RefSeq ID for the sequence that represents the chromosome this gene is on, starts with "NC_". No version suffix.';
COMMENT ON COLUMN gene.pharmgkbId IS 'The ID for this gene in PharmGKB.';
COMMENT ON COLUMN gene.allelesLastModified IS 'The date that the allele definitions for this gene were last modified.';
COMMENT ON COLUMN gene.functionalityReferenceLastModified IS 'The date that the functionality reference data for this gene was last modified';
COMMENT ON COLUMN gene.hgncId IS 'The HGNC numerical ID number for this gene prefixed by "HGNC:"';
COMMENT ON COLUMN gene.ncbiId IS 'The NCBI Gene (Entrez) ID number for this gene';
COMMENT ON COLUMN gene.ensemblId IS 'The Ensembl ID for this gene';


CREATE TABLE allele
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  name VARCHAR(200) NOT NULL,
  functionalStatus VARCHAR(200),
  pharmvarId VARCHAR(50)
);

CREATE TRIGGER version_allele
  BEFORE UPDATE ON allele
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE allele IS 'An allele of a gene';
COMMENT ON COLUMN allele.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN allele.geneSymbol IS 'The HGNC symbol of the gene the allele is for, required';
COMMENT ON COLUMN allele.name IS 'The name of this allele, required';
COMMENT ON COLUMN allele.functionalStatus IS 'The functional phenotype of this allele';
COMMENT ON COLUMN allele.pharmvarId IS 'The PharmVar core allele ID for this allele';


CREATE TABLE sequence_location
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  name VARCHAR(200),
  chromosomeLocation VARCHAR(200),
  geneLocation VARCHAR(200),
  proteinLocation VARCHAR(200),
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  dbSnpId VARCHAR(20)
);

CREATE TRIGGER version_sequence_location
  BEFORE UPDATE ON sequence_location
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE sequence_location IS 'A location on a sequence';
COMMENT ON COLUMN sequence_location.name IS 'The short name of this sequence location, arbitrary but often the gene location and nucleotide change';
COMMENT ON COLUMN sequence_location.chromosomeLocation IS 'The partial HGVS representation of the location on the chromosomal sequence';
COMMENT ON COLUMN sequence_location.geneLocation IS 'The partial HGVS representation of the location on the gene sequence';
COMMENT ON COLUMN sequence_location.proteinLocation IS 'The partial HGVS representation of the location on the protein sequence';
COMMENT ON COLUMN sequence_location.geneSymbol IS 'The HGNC symbol fo the gene this squence location falls in';
COMMENT ON COLUMN sequence_location.dbSnpId IS 'The DBSNP identifier (rs#) for this location, optional';


CREATE TABLE allele_location_value
(
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  locationid INTEGER NOT NULL REFERENCES sequence_location(id),
  variantAllele VARCHAR(200) NOT NULL,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_pallele_location_value
  BEFORE UPDATE ON allele_location_value
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE allele_location_value IS 'The change at a specific location for a specific allele';


CREATE TABLE translation_note
(
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  note TEXT NOT NULL,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_translation_note
  BEFORE UPDATE ON translation_note
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE translation_note IS 'A note about allele translation for a gene';
COMMENT ON COLUMN translation_note.geneSymbol IS 'The HGNC gene symbol for the gene this note is about, required';
COMMENT ON COLUMN translation_note.note IS 'The text of the note about allele translation, required';


CREATE TABLE population
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  citation VARCHAR(200),
  ethnicity VARCHAR(50) NOT NULL,
  population VARCHAR(200),
  populationInfo VARCHAR(500),
  subjectType VARCHAR(500),
  subjectCount INTEGER DEFAULT 0,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_population
  BEFORE UPDATE ON population
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE population IS 'A population description of a group of subjects';
COMMENT ON COLUMN population.id IS 'A synthetic primary key ID';
COMMENT ON COLUMN population.citation IS 'A PubMed ID for a citation of this population';
COMMENT ON COLUMN population.ethnicity IS 'The major ethnicity grouping of this population';
COMMENT ON COLUMN population.population IS 'The descriptive name of this population (optional)';
COMMENT ON COLUMN population.populationInfo IS 'Further information about this population (optional)';
COMMENT ON COLUMN population.subjectType IS 'Information about the types of subjects in this population';
COMMENT ON COLUMN population.subjectCount IS 'The total number of subjects in this population';
COMMENT ON COLUMN population.version IS 'The version number, iterates on modification';


CREATE TABLE allele_frequency
(
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  population INTEGER NOT NULL REFERENCES population(id),
  frequency NUMERIC,
  label VARCHAR(50),
  version INTEGER DEFAULT 1,

  UNIQUE (alleleid, population)
);

CREATE TRIGGER version_allele_frequency
  BEFORE UPDATE ON allele_frequency
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE allele_frequency IS 'A frequency observation for a particular allele in a particular population';
COMMENT ON COLUMN allele_frequency.alleleid IS 'The ID of the allele for this observation';
COMMENT ON COLUMN allele_frequency.population IS 'The ID of the population for this observation';
COMMENT ON COLUMN allele_frequency.frequency IS 'The numeric representation of this frequency';
COMMENT ON COLUMN allele_frequency.label IS 'The textual label for this frequency, "-" means no observation made';
COMMENT ON COLUMN allele_frequency.version IS 'The version number, iterates on modification';


CREATE TABLE function_reference
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  pmid VARCHAR(50),
  finding TEXT,
  substrate_in_vitro TEXT[],
  substrate_in_vivo TEXT[],
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_function_reference
  BEFORE UPDATE ON function_reference
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE function_reference IS 'A reference about this allele''s function with supporting info';
COMMENT ON COLUMN function_reference.id IS 'The synthetic primary key for this reference';
COMMENT ON COLUMN function_reference.alleleId IS 'The ID of the allele in the allele table this function is for';
COMMENT ON COLUMN function_reference.pmid IS 'The PubMed ID of the citation for this function';
COMMENT ON COLUMN function_reference.finding IS 'A finding form the publication';
COMMENT ON COLUMN function_reference.substrate_in_vitro IS 'in vitro substrate';
COMMENT ON COLUMN function_reference.substrate_in_vivo IS 'in vivo substrate';
COMMENT ON COLUMN function_reference.version IS 'The version number, iterates on modification';


CREATE TABLE drug
(
  drugId VARCHAR(20) PRIMARY KEY NOT NULL,
  name VARCHAR(200) NOT NULL UNIQUE,
  pharmgkbId VARCHAR(20),
  rxnormId VARCHAR(20),
  drugbankId VARCHAR(20),
  atcId TEXT[],
  umlsCui VARCHAR(20),
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_drug
  BEFORE UPDATE ON drug
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE drug IS 'A clinically-used drug.';
COMMENT ON COLUMN drug.drugId IS 'A unique identifier for this drug in the form "source:id" where source is some outside knowledge resource and id is their identifier, primary key';
COMMENT ON COLUMN drug.name IS 'The generic name for this drug, lower-cased, required';
COMMENT ON COLUMN drug.pharmgkbId IS 'The PharmGKB ID for this drug, optional';
COMMENT ON COLUMN drug.rxnormId IS 'The RxNorm ID for this drug, optional';
COMMENT ON COLUMN drug.drugbankId IS 'The DrugBank ID for this drug, optional';
COMMENT ON COLUMN drug.umlsCui IS 'The UMLS Concept Unique ID for this drug, optional';
COMMENT ON COLUMN drug.atcId IS 'One or more ATC IDs for this drug in an array, optional';


CREATE TABLE file_artifact
(
    id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
    entityIds TEXT[],
    type TEXT NOT NULL,
    fileName TEXT NOT NULL UNIQUE,
    url TEXT
);

COMMENT ON TABLE file_artifact IS 'File artifact information. Each entry is a different file artifact generated by the system';
COMMENT ON COLUMN file_artifact.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN file_artifact.entityIds IS 'The IDs of the entities the file is for, required';
COMMENT ON COLUMN file_artifact.type IS 'The type of file';
COMMENT ON COLUMN file_artifact.fileName IS 'The name of the file (no path)';
COMMENT ON COLUMN file_artifact.url IS 'The permanent URL of the current version of this file';


CREATE TABLE file_artifact_history
(
    id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
    fileId INTEGER REFERENCES file_artifact(id) NOT NULL,
    source TEXT NOT NULL,
    changeDate TIMESTAMP DEFAULT current_timestamp NOT NULL,
    changeMessage TEXT,

    UNIQUE(fileId, changeDate)
);

COMMENT ON TABLE file_artifact_history IS 'History information for a related file artifact';
COMMENT ON COLUMN file_artifact_history.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN file_artifact_history.fileId IS 'The ID of the related File Artifact';
COMMENT ON COLUMN file_artifact_history.source IS 'Where this history message originated from';
COMMENT ON COLUMN file_artifact_history.changeDate IS 'The date the change was applied';
COMMENT ON COLUMN file_artifact_history.changeMessage IS 'A message describing the change that occurred';


CREATE TABLE pair
(
  pairid INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  geneSymbol VARCHAR(20) REFERENCES gene(symbol),
  drugId VARCHAR(20) REFERENCES drug(drugId),
  guidelineId INTEGER REFERENCES guideline(id),
  version INTEGER DEFAULT 1,
  level VARCHAR(5) NOT NULL,
  pgkbCALevel VARCHAR(5),
  pgxTesting VARCHAR(50),
  citations TEXT[],

  UNIQUE (geneSymbol, drugId)
);

CREATE TRIGGER version_pair
  BEFORE UPDATE ON pair
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE pair IS 'A pair of a gene and a drug that is notable to CPIC';
COMMENT ON COLUMN pair.pairid IS 'A synthetic numerical id, automatically assigned, primary key';
COMMENT ON COLUMN pair.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN pair.drugId IS 'The ID of the drug in this pair, required';
COMMENT ON COLUMN pair.guidelineId IS 'The ID of a guideline this pair is described in, optional';
COMMENT ON COLUMN pair.version IS 'The version number, iterates on modification';
COMMENT ON COLUMN pair.level IS 'The CPIC level of this pair, required';
COMMENT ON COLUMN pair.pgkbCALevel IS 'The top level of PharmGKB Clinical Annotation for this pair, optional';
COMMENT ON COLUMN pair.pgxTesting IS 'The top level of PGx testing recommendation from PharmGKB label annotations, optional';
COMMENT ON COLUMN pair.citations IS 'The PMID citations in an array for this pair, optional';


CREATE TABLE term (
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  category TEXT NOT NULL,
  term TEXT NOT NULL,
  functionaldef TEXT,
  geneticdef TEXT,
  loinc TEXT,
  version INTEGER DEFAULT 1,

  UNIQUE (category, term)
);

CREATE TRIGGER version_term
  BEFORE UPDATE ON term
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE term IS 'Standardized terms for clinical PGx test results';
COMMENT ON COLUMN term.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN term.category IS 'Category group this term falls into';
COMMENT ON COLUMN term.term IS 'The term name';
COMMENT ON COLUMN term.functionaldef IS 'The functional definition of the term';
COMMENT ON COLUMN term.geneticdef IS 'The genetic definition of the term';
COMMENT ON COLUMN term.loinc IS 'The LOINC identifier for the term';


CREATE TABLE gene_phenotype
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  phenotype TEXT,
  ehrPriority TEXT,
  consultationText TEXT,
  activityScore NUMERIC,
  version INTEGER DEFAULT 1,

  UNIQUE (geneSymbol, phenotype)
);

CREATE TRIGGER version_gene_phenotype
  BEFORE UPDATE ON gene_phenotype
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE gene_phenotype IS 'Possible phenotype values for a gene';
COMMENT ON COLUMN gene_phenotype.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN gene_phenotype.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN gene_phenotype.phenotype IS 'Coded Genotype/Phenotype Summary, optional';
COMMENT ON COLUMN gene_phenotype.ehrPriority IS 'EHR Priority Result, optional';
COMMENT ON COLUMN gene_phenotype.consultationText IS 'Consultation (Interpretation) Text Provided with Test Result';
COMMENT ON COLUMN gene_phenotype.activityScore IS 'The Activity Score number, optional';

CREATE TABLE phenotype_diplotype
(
  phenotypeId INTEGER REFERENCES gene_phenotype(id) NOT NULL,
  diplotype TEXT NOT NULL,
  version INTEGER DEFAULT 1,
  
  UNIQUE (phenotypeId, diplotype)
);

CREATE TRIGGER version_phenotype_diplotype
  BEFORE UPDATE ON phenotype_diplotype
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE phenotype_diplotype IS 'Specific diplotypes that are associated with a gene phenotype';
COMMENT ON COLUMN phenotype_diplotype.phenotypeId IS 'An ID for a gene_phenotype record, required';
COMMENT ON COLUMN phenotype_diplotype.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';


CREATE TABLE recommendation
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  guidelineId INTEGER REFERENCES guideline(id),
  drugId VARCHAR(20) REFERENCES drug(drugId),
  implications TEXT,
  drug_recommendation TEXT,
  classification VARCHAR(20),
  phenotypes JSONB,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_recommendation
  BEFORE UPDATE ON recommendation
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE recommendation IS 'Recommendations for a gene phenotype pulled from a guideline';
COMMENT ON COLUMN recommendation.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN recommendation.guidelineId IS 'The guideline this recommendation appears in';
COMMENT ON COLUMN recommendation.drugId IS 'The drug this recommendation is for';
COMMENT ON COLUMN recommendation.implications IS 'Implications for phenotypic measures';
COMMENT ON COLUMN recommendation.drug_recommendation IS 'Dosing or therapeutic recommendations, depending on particular drug';
COMMENT ON COLUMN recommendation.classification IS 'Classification of recommendations, described in supplementary meterial';
COMMENT ON COLUMN recommendation.phenotypes IS 'Phenotypes that this recommendation applies to, this is a JSON Array';


CREATE TABLE test_alerts
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  cds_context TEXT NOT NULL,
  trigger_condition TEXT[],
  drugId VARCHAR(20) REFERENCES drug(drugId),
  reference_point TEXT,
  activity_score TEXT,
  alert_text TEXT[] NOT NULL,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_test_alerts
  BEFORE UPDATE ON test_alerts
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE test_alerts IS 'Example CDS test alert language';
COMMENT ON COLUMN test_alerts.id IS 'A synthetic numerical ID, primary key';
COMMENT ON COLUMN test_alerts.cds_context IS 'This should be either pre-test or post-text';
COMMENT ON COLUMN test_alerts.trigger_condition IS 'An array of one more more descriptions of trigger conditions';
COMMENT ON COLUMN test_alerts.drugId IS 'The ID of a drug this alert text is for';
COMMENT ON COLUMN test_alerts.reference_point IS 'A reference to a labeled part of the flow chart that accompanies this alert, optional';
COMMENT ON COLUMN test_alerts.activity_score IS 'A description of the activity score criteria, optional';
COMMENT ON COLUMN test_alerts.alert_text IS 'An array of one or more pieces of alert text';
