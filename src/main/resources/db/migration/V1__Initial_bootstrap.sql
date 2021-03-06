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
  name TEXT UNIQUE NOT NULL,
  url TEXT UNIQUE,
  pharmgkbId TEXT[],
  genes TEXT[]
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
COMMENT ON COLUMN guideline.genes IS 'The subject genes of this guideline';


CREATE TABLE publication
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  guidelineId INTEGER REFERENCES guideline(id),
  title TEXT UNIQUE,
  authors TEXT[],
  journal TEXT,
  month INTEGER,
  page VARCHAR(50),
  volume VARCHAR(50),
  year INTEGER,
  pmid TEXT,
  pmcid TEXT,
  doi TEXT,
  url TEXT,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_publication
  BEFORE UPDATE ON publication
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE publication IS 'Documents published to an external resource';
COMMENT ON COLUMN publication.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN publication.guidelineId IS 'The ID for the guideline this publication is about, optional';
COMMENT ON COLUMN publication.pmid IS 'The PubMed identifier for this publication';
COMMENT ON COLUMN publication.pmcid IS 'The PubMed Central identifier for this publication';
COMMENT ON COLUMN publication.doi IS 'The Document Object Identifier (DOI) for this publication';
COMMENT ON COLUMN publication.url IS 'The URL for this publication';


CREATE TABLE gene
(
  symbol            VARCHAR(20) PRIMARY KEY NOT NULL,
  chr               VARCHAR(20) CHECK (chr ~ '^chr\S+$'),
  geneSequenceId    TEXT CHECK (geneSequenceId ~ '^(NG_\S+)?$'),
  proteinSequenceId TEXT CHECK (proteinSequenceId ~ '^(NP_\S+)?$'),
  chromoSequenceId  TEXT CHECK (chromoSequenceId ~ '^(NC_\S+)?$'),
  mrnaSequenceId    TEXT CHECK (mrnaSequenceId ~ '^(NM_\S+)?$'),
  hgncId            TEXT CHECK (hgncId ~ '^(HGNC:\d+)?$'),
  ncbiId            TEXT,
  ensemblId         TEXT CHECK (ensemblId ~ '^(ENSG\d+)?$'),
  pharmgkbId        TEXT CHECK (pharmgkbId ~ '^(PA\d+)?$'),
  frequencyMethods  TEXT,
  lookupMethod      TEXT DEFAULT 'PHENOTYPE',
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
COMMENT ON COLUMN gene.mrnaSequenceId IS 'The RefSeq ID for the sequence that represents the translation of this gene, starts with "NM_". No version suffix.';
COMMENT ON COLUMN gene.pharmgkbId IS 'The ID for this gene in PharmGKB.';
COMMENT ON COLUMN gene.hgncId IS 'The HGNC numerical ID number for this gene prefixed by "HGNC:"';
COMMENT ON COLUMN gene.ncbiId IS 'The NCBI Gene (Entrez) ID number for this gene';
COMMENT ON COLUMN gene.frequencyMethods IS 'Text documentation of the methods and caveats for allele frequency data';
COMMENT ON COLUMN gene.lookupMethod IS 'The way to lookup information about diplotypes of this gene, should use ACTIVITY_SCORE, PHENOTYPE, or ALLELE_STATUS';
COMMENT ON COLUMN gene.ensemblId IS 'The Ensembl ID for this gene';


create table allele_definition
(
    id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
    version INTEGER DEFAULT 1,
    geneSymbol VARCHAR(50) NOT NULL,
    name TEXT NOT NULL,
    pharmvarId VARCHAR(50),

    constraint allele_definition_unique UNIQUE (geneSymbol, name),
    constraint allele_definitions_for_gene FOREIGN KEY (geneSymbol) REFERENCES gene(symbol)
);

CREATE TRIGGER version_allele_definition
    BEFORE UPDATE ON allele_definition
    FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE allele_definition IS 'The definition for an allele of a gene';
COMMENT ON COLUMN allele_definition.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN allele_definition.geneSymbol IS 'The HGNC symbol of the gene the allele is for, required';
COMMENT ON COLUMN allele_definition.name IS 'The name of this allele, required';
COMMENT ON COLUMN allele_definition.pharmvarId IS 'The PharmVar core allele ID for this allele';


CREATE TABLE sequence_location
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  name TEXT,
  chromosomeLocation TEXT,
  geneLocation TEXT,
  proteinLocation TEXT,
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
  alleleDefinitionId INTEGER NOT NULL REFERENCES allele_definition(id),
  locationId INTEGER NOT NULL REFERENCES sequence_location(id),
  variantAllele TEXT NOT NULL,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_allele_location_value
  BEFORE UPDATE ON allele_location_value
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE allele_location_value IS 'The change at a specific location for a specific allele';
COMMENT ON COLUMN allele_location_value.alleleDefinitionId IS 'The reference to the allele this variant is on';
COMMENT ON COLUMN allele_location_value.locationId IS 'The reference to the location this variant is for';
COMMENT ON COLUMN allele_location_value.variantAllele IS 'The allele of this location for the allele';
COMMENT ON COLUMN allele_location_value.version IS 'The version number, iterates on modification';


CREATE TABLE allele
(
    id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
    version INTEGER DEFAULT 1,
    geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
    name TEXT NOT NULL,
    functionalStatus TEXT,
    clinicalFunctionalStatus TEXT,
    clinicalFunctionalSubstrate TEXT,
    activityValue VARCHAR(50),
    definitionId INTEGER NOT NULL REFERENCES allele_definition(id),
    citations TEXT[],
    strength TEXT,
    findings JSONB,
    functionComments TEXT,

    constraint allele_unique UNIQUE (geneSymbol, name)
);

CREATE TRIGGER version_allele
    BEFORE UPDATE ON allele
    FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE allele IS 'An allele of a gene';
COMMENT ON COLUMN allele.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN allele.version IS 'The version number, iterates on modification';
COMMENT ON COLUMN allele.geneSymbol IS 'The HGNC symbol of the gene the allele is for, required';
COMMENT ON COLUMN allele.name IS 'The name of this allele, required';
COMMENT ON COLUMN allele.functionalStatus IS 'The functional phenotype of this allele';
COMMENT ON COLUMN allele.clinicalFunctionalStatus IS 'The functional phenotype of this allele used for clinical systems';
COMMENT ON COLUMN allele.clinicalFunctionalSubstrate IS 'Allele clinical function substrate specificity, optional';
COMMENT ON COLUMN allele.activityValue IS 'Descriptor of activity score, optional';
COMMENT ON COLUMN allele.definitionId IS 'The reference to the definition for this allele';
COMMENT ON COLUMN allele.citations IS 'An array of PubMed IDs use as citations for this functional assignment';
COMMENT ON COLUMN allele.strength IS 'The strength of evidence';
COMMENT ON COLUMN allele.findings IS 'Findings listed by PubMed ID';
COMMENT ON COLUMN allele.functionComments IS 'Comments this functional assignment of this allele';


CREATE TABLE population
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  publicationId INTEGER REFERENCES publication(id),
  ethnicity TEXT NOT NULL,
  population TEXT,
  populationInfo TEXT,
  subjectType TEXT,
  subjectCount INTEGER DEFAULT 0,
  version INTEGER DEFAULT 1
);

CREATE TRIGGER version_population
  BEFORE UPDATE ON population
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE population IS 'A population description of a group of subjects';
COMMENT ON COLUMN population.id IS 'A synthetic primary key ID';
COMMENT ON COLUMN population.publicationId IS 'A reference to a publication';
COMMENT ON COLUMN population.ethnicity IS 'The major ethnicity grouping of this population';
COMMENT ON COLUMN population.population IS 'The descriptive name of this population, optional';
COMMENT ON COLUMN population.populationInfo IS 'Further information about this population, optional';
COMMENT ON COLUMN population.subjectType IS 'Information about the types of subjects in this population';
COMMENT ON COLUMN population.subjectCount IS 'The total number of subjects in this population';
COMMENT ON COLUMN population.version IS 'The version number, iterates on modification';


CREATE TABLE allele_frequency
(
  alleleid INTEGER NOT NULL REFERENCES allele(id),
  population INTEGER NOT NULL REFERENCES population(id),
  frequency NUMERIC,
  label TEXT,
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


CREATE TABLE drug
(
  drugId VARCHAR(20) PRIMARY KEY NOT NULL,
  name TEXT NOT NULL UNIQUE,
  pharmgkbId VARCHAR(20),
  rxnormId VARCHAR(20),
  drugbankId VARCHAR(20),
  atcId TEXT[],
  umlsCui VARCHAR(20),
  flowChart TEXT,
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
COMMENT ON COLUMN drug.flowChart IS 'URL for the flow chart image of this drug';

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
  usedForRecommendation BOOLEAN DEFAULT FALSE,
  version INTEGER DEFAULT 1,
  cpiclevel VARCHAR(5) NOT NULL,
  pgkbCALevel VARCHAR(5),
  pgxTesting TEXT,
  citations TEXT[],
  removed BOOLEAN DEFAULT FALSE,
  removedDate DATE,
  removedReason TEXT,

  UNIQUE (geneSymbol, drugId),
  CONSTRAINT valid_cpiclevel_check CHECK (cpiclevel in ('A', 'A/B', 'B', 'B/C', 'C', 'C/D', 'D')),
  CONSTRAINT valid_pgkblevel_check CHECK ( pgkbCALevel in ('1A', '1B', '2A', '2B', '3', '4') ),
  CONSTRAINT valid_used_check CHECK ( not usedForRecommendation or guidelineId is not null )
);

CREATE TRIGGER version_pair
  BEFORE UPDATE ON pair
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE pair IS 'A pair of a gene and a drug that is notable to CPIC';
COMMENT ON COLUMN pair.pairid IS 'A synthetic numerical id, automatically assigned, primary key';
COMMENT ON COLUMN pair.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN pair.drugId IS 'The ID of the drug in this pair, required';
COMMENT ON COLUMN pair.guidelineId IS 'The ID of a guideline this pair is described in, optional';
COMMENT ON COLUMN pair.usedForRecommendation IS 'Whether the gene is used for recommendation lookup for the drug if this pair is part of a guideline, default false';
COMMENT ON COLUMN pair.version IS 'The version number, iterates on modification';
COMMENT ON COLUMN pair.cpiclevel IS 'The CPIC level of this pair, required';
COMMENT ON COLUMN pair.pgkbCALevel IS 'The top level of PharmGKB Clinical Annotation for this pair, optional';
COMMENT ON COLUMN pair.pgxTesting IS 'The top level of PGx testing recommendation from PharmGKB label annotations, optional';
COMMENT ON COLUMN pair.citations IS 'The PMID citations in an array for this pair, optional';
COMMENT ON COLUMN pair.removed IS 'Has this pair been "removed", eg is no longer a valid pair, default false';
COMMENT ON COLUMN pair.removedDate IS 'If removed, when was this pair "removed"';
COMMENT ON COLUMN pair.removedReason  IS 'If removed, why was this pair "removed"';


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


CREATE TABLE gene_result
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  result TEXT NOT NULL,
  activityScore TEXT,
  ehrPriority TEXT,
  consultationText TEXT,
  version INTEGER DEFAULT 1,

  UNIQUE (geneSymbol, result, activityScore)
);

CREATE TRIGGER version_gene_result
  BEFORE UPDATE ON gene_result
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE gene_result IS 'Possible phenotype values for a gene. The gene + phenotype + activity score should be unique.';
COMMENT ON COLUMN gene_result.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN gene_result.geneSymbol IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN gene_result.result IS 'The result for a gene, can either be a phenotype or "allele status" depending on the lookup method for the gene (see lookupMethod in gene table), required';
COMMENT ON COLUMN gene_result.activityScore IS 'Activity score, optional';
COMMENT ON COLUMN gene_result.ehrPriority IS 'EHR Priority Result, optional';
COMMENT ON COLUMN gene_result.consultationText IS 'Consultation (Interpretation) Text Provided with Test Result';


CREATE TABLE gene_result_lookup
(
    id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
    phenotypeId INTEGER REFERENCES gene_result(id) NOT NULL,
    lookupKey JSONB NOT NULL,
    function1 TEXT,
    function2 TEXT,
    activityValue1 TEXT,
    activityValue2 TEXT,
    totalActivityScore TEXT,
    description TEXT
);

COMMENT ON TABLE gene_result_lookup IS 'Gene descriptions that, when combined, link to a gene result. This table is a child of gene_result.';
COMMENT ON COLUMN gene_result_lookup.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN gene_result_lookup.phenotypeId IS 'An ID referencing the gene_result this is associated with, required';
COMMENT ON COLUMN gene_result_lookup.lookupKey IS 'A normalized JSON format of the data used to lookup a diplotype. The keys of this field are either the functions, activity scores, or allele statuses depending on what the gene requires. Required.';
COMMENT ON COLUMN gene_result_lookup.function1 IS 'The first allele function';
COMMENT ON COLUMN gene_result_lookup.function2 IS 'The second allele function';
COMMENT ON COLUMN gene_result_lookup.activityValue1 IS 'The activity score for the first allele function';
COMMENT ON COLUMN gene_result_lookup.activityValue2 IS 'The activity score for the second allele function';
COMMENT ON COLUMN gene_result_lookup.totalActivityScore IS 'The sum activity score for the functions';
COMMENT ON COLUMN gene_result_lookup.description IS 'A description of the diplotypes associated with this phenotype';


CREATE TABLE gene_result_diplotype
(
    id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
    functionPhenotypeId INTEGER REFERENCES gene_result_lookup(id) NOT NULL,
    diplotype TEXT NOT NULL,
    diplotypeKey JSONB NOT NULL,

    UNIQUE (functionPhenotypeId, diplotypeKey)
);

COMMENT ON TABLE gene_result_diplotype IS 'Specific diplotypes that are associated with a gene result. This table is a child of gene_result_lookup and distantly of gene_result';
COMMENT ON COLUMN gene_result_diplotype.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN gene_result_diplotype.functionPhenotypeId IS 'An ID referencing a gene_result_lookup record, required';
COMMENT ON COLUMN gene_result_diplotype.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';
COMMENT ON COLUMN gene_result_diplotype.diplotypeKey IS 'A normalized JSON version of the diplotype for use in lookups. Should be an object with the allele names as properties and the counts as the values. Required';


CREATE TABLE recommendation
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  guidelineId INTEGER REFERENCES guideline(id),
  drugId VARCHAR(20) REFERENCES drug(drugId),
  implications JSONB,
  drugRecommendation TEXT,
  classification VARCHAR(20),
  phenotypes JSONB,
  prescribingChange TEXT,
  activityScore JSONB,
  alleleStatus JSONB,
  lookupKey JSONB,
  population TEXT,
  comments TEXT,
  version INTEGER DEFAULT 1,

  UNIQUE (guidelineId, drugId, population, lookupKey),
  CHECK ( prescribingChange in ('Yes', 'No', 'Possibly') )
);

CREATE TRIGGER version_recommendation
  BEFORE UPDATE ON recommendation
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE recommendation IS 'Recommendations for a gene result pulled from a guideline';
COMMENT ON COLUMN recommendation.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN recommendation.guidelineId IS 'The guideline this recommendation appears in';
COMMENT ON COLUMN recommendation.drugId IS 'The drug this recommendation is for';
COMMENT ON COLUMN recommendation.implications IS 'Implications for phenotypic measures, this is a JSON mapping of gene to implication';
COMMENT ON COLUMN recommendation.drugRecommendation IS 'Dosing or therapeutic recommendations, depending on particular drug';
COMMENT ON COLUMN recommendation.classification IS 'Classification of recommendations, described in supplementary meterial';
COMMENT ON COLUMN recommendation.population IS 'The population this recommendation is applicable to';
COMMENT ON COLUMN recommendation.comments IS 'Optional comments about the recommendation';
COMMENT ON COLUMN recommendation.phenotypes IS 'Phenotypes that this recommendation applies to, this is a JSON mapping of gene to phenotype';
COMMENT ON COLUMN recommendation.prescribingChange IS 'Does this recommendation include a prescribing change? values: Yes, No, Possibly. default: No';
COMMENT ON COLUMN recommendation.activityScore IS 'Activity score that this recommendation applies to, this is a JSON mapping of gene to score value';
COMMENT ON COLUMN recommendation.alleleStatus IS 'Whether or not an allele is present, used mainly for HLA genes, and used for recommendation lookups. This is a JSON mapping of gene to allele status (positive/negative)';
COMMENT ON COLUMN recommendation.lookupKey IS 'A key to use for finding a specific recommendation. Made of a JSON object of gene symbol to key value. The key value can be one of phenotype, activity score, or allele status depending on the gene.';


CREATE TABLE test_alert
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  population TEXT,
  cdsContext TEXT NOT NULL,
  genes TEXT[],
  phenotype JSONB,
  activityScore JSONB,
  alleleStatus JSONB,
  lookupKey JSONB,
  drugId VARCHAR(20) REFERENCES drug(drugId),
  alertText TEXT[] NOT NULL,
  version INTEGER DEFAULT 1,

  UNIQUE (drugId, population, lookupKey)
);

CREATE TRIGGER version_test_alert
  BEFORE UPDATE ON test_alert
  FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE test_alert IS 'Example CDS test alert language';
COMMENT ON COLUMN test_alert.id IS 'A synthetic numerical ID, primary key';
COMMENT ON COLUMN test_alert.population IS 'The population this test alert is applicable to: general, adult, pediatrics, unspecified';
COMMENT ON COLUMN test_alert.cdsContext IS 'This should be either "Pre-test", "Post-test" or "No CDS". This field is non-null';
COMMENT ON COLUMN test_alert.genes IS 'One or more genes this test alert uses for trigger conditions';
COMMENT ON COLUMN test_alert.phenotype IS 'A JSON object of gene symbol keys to phenotype description';
COMMENT ON COLUMN test_alert.activityScore IS 'A JSON object of gene symbol keys to gene activity score';
COMMENT ON COLUMN test_alert.alleleStatus IS 'Whether or not an allele is present, used mainly for HLA genes. This is a JSON mapping of gene to allele status (positive/negative)';
COMMENT ON COLUMN test_alert.lookupKey IS 'A key to use for finding a specific test alert. Made of a JSON object of gene symbol to key value. The key value can be one of phenotype, activity score, or allele status depending on the gene.';
COMMENT ON COLUMN test_alert.drugId IS 'The ID of a drug this alert text is for';
COMMENT ON COLUMN test_alert.alertText IS 'An array of one or more pieces of alert text';

CREATE TABLE change_log
(
    date DATE NOT NULL,
    type TEXT NOT NULL,
    entityId TEXT,
    note TEXT NOT NULL,
    version INTEGER DEFAULT 1
);
CREATE TRIGGER version_change_log
    BEFORE UPDATE ON change_log
    FOR EACH ROW EXECUTE PROCEDURE increment_version();
COMMENT ON TABLE change_log IS 'A dated freeform text description of a change made to a type of information and optionally for a specific entity';
COMMENT ON COLUMN change_log.date IS 'The date the log note is applicable';
COMMENT ON COLUMN change_log.type IS 'The type of data this log message is about';
COMMENT ON COLUMN change_log.entityId IS 'Optional, the specific entity this note is about. For example, could be a gene symbol or drug ID';
COMMENT ON COLUMN change_log.note IS 'The freeform text note about what the change was';
COMMENT ON COLUMN change_log.version IS 'The version number, iterates on modification';

CREATE TABLE file_note
(
    type TEXT NOT NULL,
    entityId TEXT,
    note TEXT NOT NULL,
    ordinal INTEGER NOT NULL,
    version INTEGER DEFAULT 1
);
CREATE TRIGGER version_file_note
    BEFORE UPDATE ON file_note
    FOR EACH ROW EXECUTE PROCEDURE increment_version();
COMMENT ON TABLE file_note IS 'An ordered note that should appear with the type of information specified';
COMMENT ON COLUMN file_note.type IS 'The type of information this note is about';
COMMENT ON COLUMN file_note.entityId IS 'Optional, the specific entity this note is about. For example, could be a gene symbol or drug ID';
COMMENT ON COLUMN file_note.note IS 'The freeform text note about the type/entity';
COMMENT ON COLUMN file_note.ordinal IS 'The index number for ordering notes of a given type/entity';
COMMENT ON COLUMN file_note.version IS 'The version number, iterates on modification';
