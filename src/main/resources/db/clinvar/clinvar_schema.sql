drop schema if exists clinvar cascade;

create schema clinvar;
grant usage on schema clinvar to cpic;

create table clinvar.submission
(
    VariationID                 TEXT,
    ClinicalSignificance        TEXT,
    DateLastEvaluated           TEXT,
    Description                 TEXT,
    SubmittedPhenotypeInfo      TEXT,
    ReportedPhenotypeInfo       TEXT,
    ReviewStatus                TEXT,
    CollectionMethod            TEXT,
    OriginCounts                TEXT,
    Submitter                   TEXT,
    SCV                         TEXT,
    SubmittedGeneSymbol         TEXT,
    ExplanationOfInterpretation TEXT
);
create index on clinvar.submission (submitter);
create index on clinvar.submission (variationID);


create table clinvar.allele_gene
(
    AlleleID         TEXT,
    GeneID           TEXT,
    Symbol           TEXT,
    Name             TEXT,
    GenesPerAlleleID TEXT,
    Category         TEXT,
    Source           TEXT
);
create index on clinvar.allele_gene (alleleID);


create table clinvar.variation_allele
(
    VariationID TEXT,
    Type        TEXT,
    AlleleID    TEXT,
    Interpreted TEXT
);
create index on clinvar.variation_allele (variationID);
create index on clinvar.variation_allele (alleleID);


create table clinvar.variant_summary
(
    AlleleID             TEXT,
    Type                 TEXT,
    Name                 TEXT,
    GeneID               TEXT,
    GeneSymbol           TEXT,
    HGNC_ID              TEXT,
    ClinicalSignificance TEXT,
    ClinSigSimple        TEXT,
    LastEvaluated        TEXT,
    RSID                 TEXT,
    DBVARID              TEXT,
    RCVaccession         TEXT,
    PhenotypeIDS         TEXT,
    PhenotypeList        TEXT,
    Origin               TEXT,
    OriginSimple         TEXT,
    Assembly             TEXT,
    ChromosomeAccession  TEXT,
    Chromosome           TEXT,
    Start                TEXT,
    Stop                 TEXT,
    ReferenceAllele      TEXT,
    AlternateAllele      TEXT,
    Cytogenetic          TEXT,
    ReviewStatus         TEXT,
    NumberSubmitters     TEXT,
    Guidelines           TEXT,
    TestedInGTR          TEXT,
    OtherIDs             TEXT,
    SubmitterCategories  TEXT,
    VariationID          TEXT,
    PositionVCF          TEXT,
    ReferenceAlleleVCF   TEXT,
    AlternateAlleleVCF   TEXT
);
create index on clinvar.variant_summary (alleleID);
create index on clinvar.variant_summary (variationID);


create table clinvar.orgtrack
(
    Name                                TEXT,
    Genes                               TEXT,
    Protein_change                      TEXT,
    Conditions                          TEXT,
    Clinical_significance_Last_reviewed TEXT,
    Review_status                       TEXT,
    Accession                           TEXT,
    GRCh37Chromosome                    TEXT,
    GRCh37Location                      TEXT,
    GRCh38Chromosome                    TEXT,
    GRCh38Location                      TEXT,
    VariationID                         TEXT,
    AlleleIDs                           TEXT,
    dbSNP_ID                            TEXT,
    Canonical_SPDI                      TEXT
);
create index on clinvar.orgtrack (Accession);
create index on clinvar.orgtrack (variationID);


grant select on all tables in schema clinvar to cpic;
