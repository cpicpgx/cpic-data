SET ROLE cpic;

CREATE SEQUENCE cpic_id START 100000;

CREATE TABLE guideline
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  name VARCHAR(200) UNIQUE NOT NULL,
  url VARCHAR(200) UNIQUE,
  pharmgkbId TEXT[]
);

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
  title VARCHAR(200) UNIQUE NOT NULL,
  authors TEXT[],
  journal varchar(200),
  month integer,
  page varchar(50),
  volume varchar(50),
  year integer,
  pmid text,
  pmcid text,
  doi text
);

COMMENT ON TABLE publication IS 'Published literature';
COMMENT ON COLUMN publication.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN publication.guidelineId IS 'The ID for the guideline this publication is about, optional';
COMMENT ON COLUMN publication.pmid IS 'The PubMed identifier for this publication';
COMMENT ON COLUMN publication.pmcid IS 'The PubMed Central identifier for this publication';
COMMENT ON COLUMN publication.doi IS 'The Document Object Identifier (DOI) for this publication';

-- guideline publications
copy publication(title,authors,journal,month,page,volume,year,pmid) from STDIN;
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for CYP2D6 Genotype and Use of Ondansetron and Tropisetron	{"Bell Gillian C","Caudle Kelly E","Whirl-Carrillo Michelle","Gordon Ronald J","Hikino Keiko","Prows Cynthia A","Gaedigk Andrea","Agundez Jose A G","Sadhasivam Senthilkumar","Klein Teri E","Schwab Matthias"}	Clinical pharmacology and therapeutics	12			2016	28002639
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for CYP2D6 and CYP2C19 Genotypes and Dosing of Selective Serotonin Reuptake Inhibitors	{"Hicks J Kevin","Bishop Jeffrey R","Sangkuhl Katrin","Müller Daniel J","Ji Yuan","Leckband Susan G","Leeder J Steven","Graham Rebecca L","Chiulli Dana L","LLerena Adrián","Skaar Todd C","Scott Stuart A","Stingl Julia C","Klein Teri E","Caudle Kelly E","Gaedigk Andrea"}	Clinical pharmacology and therapeutics	5			2015	25974703
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for UGT1A1 and Atazanavir Prescribing	{"Gammal Roseann S","Court Michael H","Haidar Cyrine E","Iwuchukwu Otito Frances","Gaur Aditya H","Alvarellos Maria","Guillemette Chantal","Lennox Jeffrey L","Whirl-Carrillo Michelle","Brummel Sean","Ratain Mark J","Klein Teri E","Schackman Bruce R","Caudle Kelly E","Haas David W"}	Clinical pharmacology and therapeutics	9			2015	26417955
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for the use of potent volatile anesthetic agents and succinylcholine in the context of RYR1 or CACNA1S genotypes	{"Gonsalves Stephen G","Dirksen Robert T","Sangkuhl Katrin","Pulk Rebecca","Alvarellos Maria","Vo Teresa","Hikino Keiko","Roden Dan","Klein Teri","Mark Poler S","Patel Sephalie","Caudle Kelly E","Gordon Ronald","Brandom Barbara","Biesecker Leslie G"}	Clinical pharmacology and therapeutics	11			2018	30499100
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guidelines for CYP2C9 and HLA-B Genotype and Phenytoin Dosing	{"Caudle Kelly E","Rettie Allan E","Whirl-Carrillo Michelle","Smith Lisa H","Mintzer Scott E","Lee Ming Ta Michael","Klein Teri E","Callaghan J Thomas"}	Clinical pharmacology and therapeutics	8			2014	25099164
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guidelines for HLA-B Genotype and Carbamazepine Dosing	{"Leckband Susan G","Kelsoe John R","Dunnenberger H Mark","George Alfred L","Tran Eric","Berger Reisel","Müller Daniel J","Whirl-Carrillo Michelle","Caudle Kelly E","Pirmohamed Munir"}	Clinical pharmacology and therapeutics	5			2013	23695185
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guidelines for Ivacaftor Therapy in the Context of CFTR Genotype	{"Clancy John P","Johnson Samuel G","Yee Sook Wah","McDonagh Ellen M","Caudle Kelly E","Klein Teri E","Cannavo Matthew","Giacomini Kathleen M"}	Clinical pharmacology and therapeutics	3			2014	24598717
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guidelines for Rasburicase Therapy in the context of G6PD Deficiency Genotype	{"Relling Mary V","McDonagh Ellen M","Chang Tamara","Caudle Kelly E","McLeod Howard L","Haidar Cyrine E","Klein Teri","Luzzatto Lucio"}	Clinical pharmacology and therapeutics	5			2014	24787449
Clinical Pharmacogenetics Implementation Consortium (CPIC) guidelines for IFNL3 (IL28B) genotype and peginterferon alpha based regimens	{"Muir Andrew J","Gong Li","Johnson Samuel G","Michael Lee Ming Ta","Williams Marc S","Klein Teri E","Caudle Kelly E","Nelson David R"}	Clinical pharmacology and therapeutics	10			2013	24096968
Clinical Pharmacogenetics Implementation Consortium (CPIC®) Guideline for CYP2C19 and Voriconazole Therapy	{"Moriyama Brad","Obeng Aniwaa Owusu","Barbarino Julia","Penzak Scott R","Henning Stacey A","Scott Stuart A","Agúndez José A G","Wingard John R","McLeod Howard L","Klein Teri E","Cross Shane","Caudle Kelly E","Walsh Thomas J"}	Clinical pharmacology and therapeutics	12			2016	27981572
Clinical pharmacogenetics implementation consortium (CPIC) guidelines for CYP3A5 genotype and tacrolimus dosing	{"Birdwell Kelly A","Decker Brian","Barbarino Julia M","Peterson Josh F","Stein C Michael","Sadee Wolfgang","Wang Danxin","Vinks Alexander A","He Yijing","Swen Jesse J","Leeder J Steven","van Schaik R H N","Thummel Kenneth E","Klein Teri E","Caudle Kelly E","MacPhee Iain A M"}	Clinical pharmacology and therapeutics	3			2015	25801146
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for CYP2D6 and Tamoxifen Therapy.	{"Goetz Matthew P","Sangkuhl Katrin","Guchelaar Henk-Jan","Schwab Matthias","Province Michael","Whirl-Carrillo Michelle","Symmans W Fraser","McLeod Howard L","Ratain Mark J","Zembutsu Hitoshi","Gaedigk Andrea","van Schaik Ron H","Ingle James N","Caudle Kelly E","Klein Teri E"}	Clinical pharmacology and therapeutics	5			2018	29385237
Clinical Pharmacogenetics Implementation Consortium Guideline for CYP2D6 and CYP2C19 Genotypes and Dosing of Tricyclic Antidepressants	{"Hicks J K","Swen J J","Thorn C F","Sangkuhl K","Kharasch E D","Ellingrod V L","Skaar T C","Müller D J","Gaedigk A","Stingl J C"}	Clinical pharmacology and therapeutics	1			2013	23486447
Clinical Pharmacogenetics Implementation Consortium Guideline (CPIC®) for CYP2D6 and CYP2C19 Genotypes and Dosing of Tricyclic Antidepressants: 2016 Update	{"Kevin Hicks J","Sangkuhl Katrin","Swen Jesse J","Ellingrod Vicki L","Müller Daniel J","Shimoda Kazutaka","Bishop Jeffrey R","Kharasch Evan D","Skaar Todd C","Gaedigk Andrea","Dunnenberger Henry M","Klein Teri E","Caudle Kelly E","Stingl Julia C"}	Clinical pharmacology and therapeutics	12			2016	27997040
Clinical Pharmacogenetics Implementation Consortium Guidelines for Cytochrome P450-2C19 (CYP2C19) Genotype and Clopidogrel Therapy	{"Scott S A","Sangkuhl K","Gardner E E","Stein C M","Hulot J-S","Johnson J A","Roden D M","Klein T E","Shuldiner A R"}	Clinical pharmacology and therapeutics	6			2011	21716271
Clinical Pharmacogenetics Implementation Consortium (CPIC) guidelines for cytochrome P450-2C19 (CYP2C19) genotype and clopidogrel therapy: 2013 Update	{"Scott Stuart A","Sangkuhl Katrin","Stein C Michael","Hulot Jean-Sébastien","Mega Jessica L","Roden Dan M","Klein Teri E","Sabatine Marc S","Johnson Julie A","Shuldiner Alan R"}	Clinical pharmacology and therapeutics	5			2013	23698643
Clinical Pharmacogenetics Implementation Consortium Guidelines for Dihydropyrimidine Dehydrogenase Genotype and Fluoropyrimidine Dosing	{"Caudle Kelly E","Thorn Caroline F","Klein Teri E","Swen Jesse J","McLeod Howard L","Diasio Robert B","Schwab Matthias"}	Clinical pharmacology and therapeutics	8			2013	23988873
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for Dihydropyrimidine Dehydrogenase Genotype and Fluoropyrimidine Dosing: 2017 Update	{"Amstutz Ursula","Henricks Linda M","Offer Steven M","Barbarino Julia","Schellens Jan H M","Swen Jesse J","Klein Teri E","McLeod Howard L","Caudle Kelly E","Diasio Robert B","Schwab Matthias"}	Clinical pharmacology and therapeutics	11			2017	29152729
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guidelines for Codeine Therapy in the Context of Cytochrome P450 2D6 (CYP2D6) Genotype	{"Crews K R","Gaedigk A","Dunnenberger H M","Klein T E","Shen D D","Callaghan J T","Kharasch E D","Skaar T C"}	Clinical pharmacology and therapeutics	12			2011	22205192
Clinical Pharmacogenetics Implementation Consortium (CPIC) guidelines for cytochrome P450 2D6 (CYP2D6) genotype and codeine therapy: 2014 Update	{"Crews Kristine R","Gaedigk Andrea","Dunnenberger Henry M","Leeder J Steve","Klein Teri E","Caudle Kelly E","Haidar Cyrine E","Shen Danny D","Callaghan John T","Sadhasivam Senthilkumar","Prows Cynthia A","Kharasch Evan D","Skaar Todd C"}	Clinical pharmacology and therapeutics	1			2014	24458010
Clinical Pharmacogenetics Implementation Consortium Guidelines for HLA-B Genotype and Abacavir Dosing	{"Martin M A","Klein T E","Dong B J","Pirmohamed M","Haas D W","Kroetz D L"}	Clinical pharmacology and therapeutics	2			2012	22378157
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guidelines for HLA-B Genotype and Abacavir Dosing: 2014 update	{"Martin Michael A","Hoffman James M","Freimuth Robert R","Klein Teri E","Dong Betty J","Pirmohamed Munir","Hicks J Kevin","Wilkinson Mark R","Haas David W","Kroetz Deanna L"}	Clinical pharmacology and therapeutics	2			2014	24561393
Clinical Pharmacogenetics Implementation Consortium Guidelines for Human Leukocyte Antigen-B Genotype and Allopurinol Dosing	{"Hershfield M S","Callaghan J T","Tassaneeyakul W","Mushiroda T","Thorn C F","Klein T E","Lee M T M"}	Clinical pharmacology and therapeutics	10			2012	23232549
Clinical Pharmacogenetics Implementation Consortium (CPIC) guidelines for human leukocyte antigen B (HLA-B) genotype and allopurinol dosing: 2015 update	{"Saito Y","Stamp L K","Caudle K E","Hershfield M S","McDonagh E M","Callaghan J T","Tassaneeyakul W","Mushiroda T","Kamatani N","Goldspiel B R","Phillips E J","Klein T E","Lee Mtm"}	Clinical pharmacology and therapeutics	6			2015	26094938
Clinical pharmacogenetics implementation consortium guidelines for thiopurine methyltransferase genotype and thiopurine dosing	{"Relling M V","Gardner E E","Sandborn W J","Schmiegelow K","Pui C-H","Yee S W","Stein C M","Carrillo M","Evans W E","Klein T E"}	Clinical pharmacology and therapeutics	3	387-91	89	2011	21270794
Clinical Pharmacogenetics Implementation Consortium Guidelines for Thiopurine Methyltransferase Genotype and Thiopurine Dosing: 2013 Update	{"Relling M V","Gardner E E","Sandborn W J","Schmiegelow K","Pui C-H","Yee S W","Stein C M","Carrillo M","Evans W E","Hicks J K","Schwab M","Klein T E"}	Clinical pharmacology and therapeutics	1			2013	23422873
Clinical Pharmacogenetics Implementation Consortium Guidelines for CYP2C9 and VKORC1 Genotypes and Warfarin Dosing	{"Johnson J A","Gong L","Whirl-Carrillo M","Gage B F","Scott S A","Stein C M","Anderson J L","Kimmel S E","Lee M T M","Pirmohamed M","Wadelius M","Klein T E","Altman R B"}	Clinical pharmacology and therapeutics	9			2011	21900891
Clinical pharmacogenetics implementation consortium (cpic) guideline for pharmacogenetics-guided warfarin dosing: 2017 update	{"Johnson Julie A","Caudle Kelly E","Gong Li","Whirl-Carrillo Michelle","Stein C Michael","Scott Stuart A","Lee Ming Ta Michael","Gage Brian F","Kimmel Stephen E","Perera Minoli A","Anderson Jeffrey L","Pirmohamed Munir","Klein Teri E","Limdi Nita A","Cavallari Larisa H","Wadelius Mia"}	Clinical pharmacology and therapeutics	2			2017	28198005
The Clinical Pharmacogenomics Implementation Consortium: Guideline for SLCO1B1 and Simvastatin-Induced Myopathy	{"Wilke R A","Ramsey L B","Johnson S G","Maxwell W D","McLeod H L","Voora D","Krauss R M","Roden D M","Feng Q","Cooper-Dehoff R M","Gong L","Klein T E","Wadelius M","Niemi M"}	Clinical pharmacology and therapeutics	5			2012	22617227
The Clinical Pharmacogenetics Implementation Consortium (CPIC) guideline for SLCO1B1 and simvastatin-induced myopathy: 2014 update	{"Ramsey Laura B","Johnson Samuel G","Caudle Kelly E","Haidar Cyrine E","Voora Deepak","Wilke Russell A","Maxwell Whitney D","McLeod Howard L","Krauss Ronald M","Roden Dan M","Feng Qiping","Cooper-DeHoff Rhonda M","Gong Li","Klein Teri E","Wadelius Mia","Niemi Mikko"}	Clinical pharmacology and therapeutics	6			2014	24918167
Clinical Pharmacogenetics Implementation Consortium (CPIC) guideline for thiopurine dosing based on TPMT and NUDT15 genotypes: 2018 update.	{"Relling Mary V","Schwab Matthias","Whirl-Carrillo Michelle","Suarez-Kurtz Guilherme","Pui Ching-Hon","Stein Charles M","Moyer Ann M","Evans William E","Klein Teri E","Antillon-Klussmann Federico Guillermo","Caudle Kelly E","Kato Motohiro","Yeoh Allen E J","Schmiegelow Kjeld","Yang Jun J"}	Clinical pharmacology and therapeutics	11			2018	30447069
Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline for CYP2D6 Genotype and Atomoxetine Therapy	{"Brown Jacob T", "Bishop Jeffrey R", "Sangkuhl Katrin", "Nurmi Erika L", "Mueller Daniel J", "Dinh Jean C", "Gaedigk Andrea", "Klein Teri E", "Caudle Kelly E", "McCracken James T", "de Leon Jose", "Steven Leeder J"}	Clinical pharmacology and therapeutics	2			2019	30801677
\.

-- publications about CPIC itself
copy publication(title,authors,journal,month,page,volume,year,pmid) from STDIN;
CPIC: Clinical Pharmacogenetics Implementation Consortium of the Pharmacogenomics Research Network	{"Relling M V","Klein T E"}	Clinical pharmacology and therapeutics	3	464-7	89	2011	21270786
Incorporation of Pharmacogenomics into Routine Clinical Practice: the Clinical Pharmacogenetics Implementation Consortium (CPIC) Guideline Development Process	{"Caudle Kelly E","Klein Teri E","Hoffman James M","Müller Daniel J","Whirl-Carrillo Michelle","Gong Li","McDonagh Ellen M","Sangkuhl Katrin","Thorn Caroline F","Schwab Matthias","Agunder Jose A G","Freimuth Robert R","Huser Vojtech","Lee Ming Ta Michael","Iwuchukwu Otito F","Crews Kristine R","Scott Stuart A","Wadelius Mia","Swen Jesse J","Tyndale Rachel F","Stein C Michael","Roden Dan","Relling Mary V","Williams Marc S","Johnson Samuel G"}	Current drug metabolism	1			2014	24479687
Standardizing terms for clinical pharmacogenetic test results: consensus terms from the Clinical Pharmacogenetics Implementation Consortium (CPIC)	{"Caudle Kelly E","Dunnenberger Henry M","Freimuth Robert R","Peterson Josh F","Burlison Jonathan D","Whirl-Carrillo Michelle","Scott Stuart A","Rehm Heidi L","Williams Marc S","Klein Teri E","Relling Mary V","Hoffman James M"}	Genetics in medicine : official journal of the American College of Medical Genetics	7			2016	27441996
\.


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
  functionalityReferenceLastModified  DATE
);

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

copy gene(symbol,chr,geneSequenceId,proteinSequenceId,pharmgkbId) from STDIN;
ABCB1	chr7	NG_011513	NP_000918	PA267
ABCC4	chr13		NP_005836	PA397
ABCG2	chr4		NP_004818	PA390
ABL2	chr1		NP_005149	PA24414
ACE	chr17	NG_011648	NP_000780	PA139
ADD1	chr4	NG_012037	NP_001110	PA31
ADORA2A	chr22		NP_000666	PA24584
ADRB2	chr5	NG_016421	NP_000015	PA39
ANKK1	chr11	NG_012976	NP_848605	PA134872551
APOE	chr19	NG_007084	NP_000032	PA55
ASL	chr7	NG_009288	NP_000039	PA25046
ASS1	chr9	NG_011542	NP_000041	PA162376926
ATIC	chr2	NG_013002	NP_004035	PA25094
BCHE	chr3	NG_009031	NP_000046	PA25294
C11orf65	chr11		NP_689800	PA144596484
C8orf34	chr8		NP_443190	PA142672353
CACNA1S	chr1	NG_009816	NP_000060	PA85
CALU	chr7		NP_001210	PA26047
CBR3	chr21		NP_001227	PA26122
CCHCR1	chr6		NP_061925	PA134942738
CES1	chr16	NG_012057	NP_001257	PA107
CETP	chr16	NG_008952	NP_000069	PA108
CFTR	chr7	NG_016465	NP_000483	PA109
CHRNA3	chr15	NG_016143	NP_000734	PA113
COL22A1	chr8		NP_690848	PA134914705
COMT	chr22	NG_011526	NP_000745	PA117
COQ2	chr4	NG_015825	NP_056512	PA142672084
CPS1	chr2	NG_008285	NP_001866	PA26840
CRHR1	chr17	NG_009902	NP_004373	PA26874
CRHR2	chr7		NP_001874	PA26875
CYB5R1	chr1		NP_057327	PA134979668
CYB5R2	chr11		NP_057313	PA142672060
CYB5R3	chr22	NG_012194	NP_000389	PA27331
CYB5R4	chr6		NP_057314	PA134904907
CYP2A7P1	chr19	NG_000008		PA27103
CYP2B6	chr19	NG_000008	NP_000758	PA123
CYP2C19	chr10	NG_008384	NP_000760	PA124
CYP2C8	chr10	NG_007972	NP_000761	PA125
CYP2C9	chr10	NG_008385	NP_000762	PA126
CYP2D6	chr22	NG_003180	NP_000097	PA128
CYP3A4	chr7	NG_000004	NP_059488	PA130
CYP3A5	chr7	NG_000004	NP_000768	PA131
CYP4F2	chr19	NG_007971	NP_001073	PA27121
DPYD	chr1	NG_008807	NP_000101	PA145
DRD2	chr11	NG_008841	NP_000786	PA27478
DYNC2H1	chr11	NG_016423	NP_001368	PA27433
EGF	chr4	NG_011441	NP_001954	PA27664
EPHX1	chr1	NG_009776	NP_000111	PA27829
ERCC1	chr19	NG_015839	NP_001974	PA155
F5	chr1	NG_011806	NP_000121	PA159
FCGR3A	chr1	NG_009066	NP_000560	PA28065
FDPS	chr1		NP_001995	PA28075
FKBP5	chr6	NG_012645	NP_004108	PA28162
FLOT1	chr6		NP_005794	PA28175
G6PD	chrX	NG_009015	NP_000393	PA28469
GBA	chr1	NG_009783	NP_000148	PA28591
GGCX	chr2	NG_011811	NP_000812	PA28660
GNB3	chr12	NG_009100	NP_002066	PA176
GP1BA	chr17	NG_008767	NP_000164	PA178
GRIK4	chr11		NP_055434	PA28976
GSTM1	chr1	NG_009246	NP_000552	PA182
GSTP1	chr11	NG_012075	NP_000843	PA29028
HAS3	chr16		NP_005320	PA29196
HLA-A	chr6	NG_002398	NP_002107	PA35055
HLA-B	chr6	NG_002397	NP_005505	PA35056
HLA-C	chr6	NG_002397	NP_002108	PA35057
HLA-DPB1	chr6		NP_002112	PA35064
HLA-DQA1	chr6		NP_002113	PA35066
HLA-DRB1	chr6	NG_002392	NP_002115	PA35072
HMGCR	chr5	NG_011449	NP_000850	PA189
HPRT1	chrX	NG_012329	NP_000185	PA29427
HTR1A	chr5		NP_000515	PA192
HTR2A	chr13	NG_013011	NP_000612	PA193
HTR2C	chrX	NG_012082	NP_000859	PA194
IFNL3	chr19		NP_742151	PA134952671
IFNL4	chr19			PA166049147
ITPA	chr20	NG_012093	NP_258412	PA29973
KCNIP4	chr4		NP_671711	PA134893552
KIF6	chr6		NP_659464	PA134920075
LDLR	chr19	NG_009060	NP_000518	PA227
LPA	chr6	NG_016147	NP_005568	PA30432
LTC4S	chr5		NP_665874	PA235
MC4R	chr18	NG_016441	NP_005903	PA30676
MT-RNR1	chrM			PA31274
MTHFR	chr1	NG_013351	NP_005948	PA245
MTRR	chr5	NG_008856	NP_002445	PA31277
NAGS	chr17	NG_008106	NP_694551	PA134968729
NAT1	chr8	NG_012245	NP_000653	PA17
NAT2	chr8	NG_012246	NP_000006	PA18
NEDD4L	chr18		NP_056092	PA31534
NQO1	chr16	NG_011504	NP_000894	PA31744
NT5C2	chr10		NP_036361	PA31801
NUDT15	chr13		NP_060753	PA134963132
OPRM1	chr6	NG_021208	NP_000905	PA31945
OTC	chrX	NG_008471	NP_000522	PA32840
POLG	chr15	NG_008218	NP_002684	PA33500
PRKCA	chr17	NG_012206	NP_002728	PA33759
PROC	chr2	NG_016323	NP_000303	PA33799
PROS1	chr3	NG_009813	NP_000304	PA33809
PTGFR	chr1		NP_000950	PA290
PTGS1	chr9		NP_000953	PA24346
RYR1	chr19	NG_008866	NP_000531	PA34896
SCN1A	chr2	NG_011906	NP_008851	PA301
SEMA3C	chr7		NP_006370	PA35647
SERPINC1	chr1	NG_012462	NP_000479	PA35026
SLC28A3	chr9		NP_071410	PA426
SLC47A2	chr17		NP_001093116	PA162403847
SLC6A4	chr17	NG_011747	NP_001036	PA312
SLCO1B1	chr12	NG_011745	NP_006437	PA134865839
SOD2	chr6	NG_008729	NP_000627	PA36017
TANC1	chr2		NP_203752	PA142670838
TCF7L2	chr10	NG_012631	NP_110383	PA36394
TMEM43	chr3	NG_008975	NP_077310	PA134871907
TNF	chr6	NG_007462	NP_000585	PA435
TP53	chr17	NG_017013	NP_000537	PA36679
TPMT	chr6	NG_012137	NP_000358	PA356
TXNRD2	chr22	NG_011835	NP_006431	PA38302
TYMS	chr18		NP_001062	PA359
UGT1A1	chr2	NG_002601	NP_000454	PA420
UGT1A4	chr2	NG_002601	NP_009051	PA37179
UGT2B15	chr4		NP_001067	PA37188
UMPS	chr3	NG_017037	NP_000364	PA363
VDR	chr12	NG_008731	NP_000367	PA37301
VKORC1	chr16	NG_011564	NP_076869	PA133787052
XPC	chr3	NG_011763	NP_004619	PA37413
XRCC1	chr19		NP_006288	PA369
YEATS4	chr12		NP_006521	PA134992686
\.


CREATE TABLE allele
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  version INTEGER DEFAULT 1,
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  name VARCHAR(200) NOT NULL,
  functionalStatus VARCHAR(200)
);

COMMENT ON TABLE allele IS 'An allele of a gene';
COMMENT ON COLUMN allele.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN allele.geneSymbol IS 'The HGNC symbol of the gene the allele is for, required';
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
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  dbSnpId VARCHAR(20)
);

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
  variantAllele VARCHAR(200) NOT NULL
);

COMMENT ON TABLE allele_location_value IS 'The change at a specific location for a specific allele';


CREATE TABLE translation_note
(
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  note TEXT NOT NULL
);

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
  allele_function TEXT,
  pmid VARCHAR(50),
  finding TEXT,
  substrate_in_vitro TEXT[],
  substrate_in_vivo TEXT[]
);


CREATE TABLE drug
(
  drugId VARCHAR(20) PRIMARY KEY NOT NULL,
  name VARCHAR(200) NOT NULL UNIQUE,
  pharmgkbId VARCHAR(20),
  rxnormId VARCHAR(20),
  drugbankId VARCHAR(20),
  atcId TEXT[]
);

COMMENT ON TABLE drug IS 'A clinically-used drug.';
COMMENT ON COLUMN drug.drugId IS 'A unique identifier for this drug in the form "source:id" where source is some outside knowledge resource and id is their identifier, primary key';
COMMENT ON COLUMN drug.name IS 'The generic name for this drug, lower-cased, required';
COMMENT ON COLUMN drug.pharmgkbId IS 'The PharmGKB ID for this drug, optional';
COMMENT ON COLUMN drug.rxnormId IS 'The RxNorm ID for this drug, optional';
COMMENT ON COLUMN drug.drugbankId IS 'The DrugBank ID for this drug, optional';
COMMENT ON COLUMN drug.atcId IS 'One or more ATC IDs for this drug in an array, optional';

copy drug(drugId,name,pharmgkbId,rxnormId,drugbankId,atcId) from STDIN;
RxNorm:190521	abacavir	PA448004	190521	DB01048	{"J05AF06","J05AR02","J05AR13","J05AR04"}
ATC:C09AA	Ace Inhibitors, Plain	PA164712308			{"C09AA"}
RxNorm:154	acenocoumarol	PA452632	154	DB01418	{"B01AA07"}
RxNorm:480	alfentanil	PA448084	480	DB00802	{"N01AH02"}
ATC:L01A	Alkylating Agents	PA164712331			{"L01A"}
RxNorm:519	allopurinol	PA448320	519	DB00437	{"M04AA01","M04AA51"}
ATC:J01G	aminoglycoside antibacterials	PA452167			{"J01G"}
RxNorm:704	amitriptyline	PA448385	704	DB00321	{"N06AA09","N06CA01"}
ATC:L01DB	anthracyclines and related substances	PA130620651			{"L01DB"}
ATC:N06A	antidepressants	PA452229			{"N06A"}
ATC:N05A	antipsychotics	PA452233			{"N05A"}
RxNorm:89013	aripiprazole	PA10026	89013	DB01238	{"N05AX12"}
RxNorm:1191	aspirin	PA448497	1191	DB00945	{"A01AD05","B01AC06","N02BA01"}
MeSH:C515878	ataluren	PA166151864			{"M09AX03"}
RxNorm:343047	atazanavir	PA10251	343047	DB01072	{"J05AE08","J05AR15"}
RxNorm:38400	atomoxetine	PA134688071	38400	DB00289	{"N06BA09"}
RxNorm:83367	atorvastatin	PA448500	83367	DB01076	{"C10AA05"}
RxNorm:1256	azathioprine	PA448515	1256	DB00993	{"L04AX01"}
RxNorm:1543543	belinostat	PA165971474	1543543		{""}
ATC:M05BA	Bisphosphonates	PA164712563			{"M05BA"}
RxNorm:1658314	brexpiprazole	PA166160053	1658314		{""}
RxNorm:1739745	brivaracetam	PA166153491	1739745		{""}
RxNorm:19831	budesonide	PA448681	19831	DB01222	{"A07EA06","D07AC09","R01AD05","R03BA02"}
RxNorm:42347	bupropion	PA448687	42347	DB01156	{"N06AX12"}
RxNorm:1886	caffeine	PA448710	1886	DB00201	{"N06BC01"}
RxNorm:194000	capecitabine	PA448771	194000	DB01101	{"L01BC06"}
RxNorm:1998	captopril	PA448780	1998	DB01197	{"C09AA01"}
RxNorm:2002	carbamazepine	PA448785	2002	DB00564	{"N03AF01"}
RxNorm:2020	carbimazole	PA164742970	2020	DB00389	{"H03BB01"}
RxNorm:40048	carboplatin	PA448803	40048	DB00958	{"L01XA02"}
RxNorm:401713	carglumic acid	PA165958402	401713	DB06775	{"A16AA05"}
RxNorm:2101	carisoprodol	PA448809	2101	DB00395	{"M03BA02"}
RxNorm:20352	carvedilol	PA448817	20352	DB01136	{"C07AG02"}
RxNorm:140587	celecoxib	PA448871	140587	DB00482	{"L01XX33","M01AH01"}
RxNorm:596723	cerivastatin	PA448897	596723	DB00439	{"C10AA06"}
RxNorm:318341	cetuximab	PA10040	318341	DB00002	{"L01XC06"}
RxNorm:44281	cevimeline	PA164754754	44281	DB00185	{"N07AX03"}
RxNorm:2348	chloramphenicol	PA448927	2348	DB00446	{"D06AX02","D10AF03","G01AA05","J01BA01","S01AA01","S02AA01","S03AA08"}
RxNorm:2393	chloroquine	PA448948	2393	DB00608	{"P01BA01"}
RxNorm:2404	chlorpropamide	PA448966	2404	DB00672	{"A10BB02"}
RxNorm:2551	ciprofloxacin	PA449009	2551	DB00537	{"J01MA02","S01AE03","S02AA15","S03AA07"}
RxNorm:2555	cisplatin	PA449014	2555	DB00515	{"L01XA01"}
RxNorm:2556	citalopram	PA449015	2556	DB00215	{"N06AB04"}
RxNorm:21241	clobazam	PA10888	21241	DB00349	{"N05BA09"}
RxNorm:2597	clomipramine	PA449048	2597	DB01242	{"N06AA04"}
RxNorm:32968	clopidogrel	PA449053	32968	DB00758	{"B01AC04"}
RxNorm:2626	clozapine	PA449061	2626	DB00363	{"N05AH02"}
RxNorm:2670	codeine	PA449088	2670	DB00318	{"R05DA04","N02AA59","N02AA79"}
RxNorm:3002	cyclophosphamide	PA449165	3002	DB00531	{"L01AA01"}
RxNorm:3008	cyclosporine	PA449167	3008	DB00091	{"L04AD01","S01XA18"}
RxNorm:1424911	dabrafenib	PA166114911	1424911		{"L01XE"}
RxNorm:3108	dapsone	PA449211	3108	DB00250	{"D10AX05","J04BA02"}
RxNorm:136198	darifenacin	PA164774901	136198	DB00496	{"G04BD10"}
RxNorm:27340	desflurane	PA164749136	27340	DB01189	{"N01AB07"}
RxNorm:3247	desipramine	PA449233	3247	DB01151	{"N06AA01"}
RxNorm:816346	dexlansoprazole	PA166110257	816346		{""}
RxNorm:3289	dextromethorphan	PA449273	3289	DB00514	{"R05DA09"}
RxNorm:3322	diazepam	PA449283	3322	DB00829	{"N05BA01"}
RxNorm:3355	diclofenac	PA449293	3355	DB00586	{"D11AX18","M01AB05","M02AA15","S01BC03"}
RxNorm:3407	digoxin	PA449319	3407	DB00390	{"C01AA05"}
RxNorm:3445	dimercaprol	PA165958406	3445	DB06782	{"V03AB09"}
RxNorm:266856	divalproex sodium	PA164783479	266856	DB00510	{""}
RxNorm:68091	dolasetron	PA449390	68091	DB00757	{"A04AA04"}
RxNorm:1433868	dolutegravir	PA166114961	1433868		{""}
RxNorm:135447	donepezil	PA449394	135447	DB00843	{"N06DA02"}
RxNorm:3638	doxepin	PA449409	3638	DB01142	{"N06AA12"}
RxNorm:72625	duloxetine	PA10066	72625	DB00476	{"N06AX21"}
RxNorm:195085	efavirenz	PA449441	195085	DB00625	{"J05AG03"}
RxNorm:1547220	eliglustat	PA166123486	1547220		{""}
RxNorm:711942	eltrombopag	PA165981594	711942		{"B02BX"}
RxNorm:3920	enflurane	PA449461	3920	DB00228	{"N01AB04"}
RxNorm:5095	halothane	PA449845	5095	DB01159	{"N01AB01"}
RxNorm:6857	methoxyflurane	PA450434	6857	DB01028	{"N02BG09"}
RxNorm:3995	epirubicin	PA449476	3995	DB00445	{"L01DB03"}
RxNorm:4053	erythromycin	PA449493	4053	DB00199	{"D10AF02","J01FA01","S01AA17"}
RxNorm:321988	escitalopram	PA10074	321988	DB01175	{"N06AB10"}
RxNorm:283742	esomeprazole	PA10075	283742	DB00736	{"A02BC05"}
RxNorm:448	ethanol	PA448073	448	DB00898	{"D08AX08","V03AB16","V03AZ01"}
RxNorm:4179	etoposide	PA449552	4179	DB00773	{"L01CB01"}
RxNorm:4337	fentanyl	PA449599	4337	DB00813	{"N01AH01","N02AB03"}
RxNorm:797195	fesoterodine	PA165958376	797195	DB06702	{"G04BD11"}
RxNorm:4441	flecainide	PA449646	4441	DB01195	{"C01BC04"}
RxNorm:1665509	flibanserin	PA166153431	1665509		{""}
RxNorm:4492	fluorouracil	PA128406956	4492	DB00544	{"L01BC02","L01BC52"}
RxNorm:4493	fluoxetine	PA449673	4493	DB00472	{"N06AB03"}
RxNorm:4502	flurbiprofen	PA449683	4502	DB00712	{"M01AE09","M02AA19","R02AX01","S01BC04"}
RxNorm:50121	fluticasone propionate	PA449686	50121	DB00588	{"D07AC17","R01AD08","R03BA05"}
RxNorm:284635	fluticasone/salmeterol	PA165290926	284635		{""}
RxNorm:42355	fluvoxamine	PA449690	42355	DB00176	{"N06AB08"}
RxNorm:4603	furosemide	PA449719	4603	DB00695	{"C03CA01"}
RxNorm:4637	galantamine	PA449726	4637	DB00674	{"N06DA04"}
RxNorm:12574	gemcitabine	PA449748	12574	DB00441	{"L01BC05"}
RxNorm:4815	glibenclamide	PA449782	4815	DB01016	{"A10BB01"}
RxNorm:25789	glimepiride	PA449761	25789	DB00222	{"A10BB12"}
RxNorm:4821	glipizide	PA449762	4821	DB01067	{"A10BB07"}
RxNorm:5093	haloperidol	PA449841	5093	DB00502	{"N05AD01"}
ATC:C10AA	hmg coa reductase inhibitors	PA133950441			{"C10AA"}
ATC:G03A	hormonal contraceptives for systemic use	PA452637			{"G03A"}
RxNorm:5470	hydralazine	PA449894	5470	DB01275	{"C02DB02"}
RxNorm:5487	hydrochlorothiazide	PA449899	5487	DB00999	{"C03AA03","C03AB03","C09DX01","C09DX03","C09XA52","C03EA01","C03AX01","G01AE10","C09XA54"}
RxNorm:5521	hydroxychloroquine	PA164777036	5521	DB01611	{"P01BA02"}
RxNorm:73178	iloperidone	PA161199368	73178		{""}
RxNorm:5691	imipramine	PA449969	5691	DB00458	{"N06AA02"}
RxNorm:5880	interferon alfa-2b, recombinant	PA164783990	5880	DB00105	{"L03AB05"}
RxNorm:51499	irinotecan	PA450085	51499	DB00762	{"L01XX19"}
RxNorm:6026	isoflurane	PA450106	6026	DB00753	{"N01AB06"}
RxNorm:6038	isoniazid	PA450112	6038	DB00951	{"J04AC01"}
RxNorm:6058	isosorbide dinitrate	PA450125	6058	DB00883	{"C01DA08","C05AE02"}
RxNorm:1243041	ivacaftor	PA165950341	1243041	DB08820	{"R07AX02","R07AX30"}
RxNorm:28439	lamotrigine	PA450164	28439	DB00555	{"N03AX09"}
RxNorm:17128	lansoprazole	PA450180	17128	DB00448	{"A02BC03"}
RxNorm:480167	lapatinib	PA152241907	480167	DB01259	{"L01XE07"}
RxNorm:43611	latanoprost	PA164774763	43611	DB00654	{"S01EE01"}
RxNorm:1731031	lesinurad	PA166160006	1731031		{""}
RxNorm:6313	leucovorin	PA450198	6313	DB00650	{"V03AF"}
RxNorm:82122	levofloxacin	PA450214	82122	DB01137	{"J01MA12","S01AE05"}
RxNorm:6387	lidocaine	PA450226	6387	DB00281	{"C01BB01","C05AD01","D04AB01","N01BB02","R02AD02","S01HA07","S02DA01"}
RxNorm:6572	mafenide	PA166114925	6572		{""}
RxNorm:6694	mefloquine	PA450348	6694	DB00358	{"P01BC02"}
RxNorm:103	mercaptopurine	PA450379	103	DB01033	{"L01BB02"}
RxNorm:52582	mesalazine	PA450384	52582	DB00244	{"A07EC02"}
RxNorm:6809	metformin	PA450395	6809	DB00331	{"A10BA02"}
RxNorm:6813	methadone	PA450401	6813	DB00333	{"N07BC02"}
RxNorm:6826	methazolamide	PA450413	6826	DB00703	{"S01EC05"}
RxNorm:6835	methimazole	PA450422	6835	DB00763	{"H03BB02"}
RxNorm:6851	methotrexate	PA450428	6851	DB00563	{"L01BA01","L04AX03"}
RxNorm:6878	methylene blue	PA450457	6878		{"V03AB","V04CG"}
RxNorm:6901	methylphenidate	PA450464	6901	DB00422	{"N06BA04"}
RxNorm:6915	metoclopramide	PA450475	6915	DB01233	{"A03FA01"}
RxNorm:6918	metoprolol	PA450480	6918	DB00264	{"C07AB02"}
RxNorm:6960	midazolam	PA450496	6960	DB00683	{"N05CD08"}
RxNorm:15996	mirtazapine	PA450522	15996	DB00370	{"N06AX11"}
RxNorm:30125	modafinil	PA450530	30125	DB00745	{"N06BA07"}
RxNorm:7052	morphine	PA450550	7052	DB00295	{"N02AA01"}
RxNorm:139462	moxifloxacin	PA450555	139462	DB00218	{"J01MA14","S01AE07"}
RxNorm:7145	mycophenolic acid	PA164748728	7145	DB01024	{"L04AA06"}
RxNorm:7240	nalidixic acid	PA164746384	7240	DB00779	{"J01MB02"}
RxNorm:7242	naloxone	PA450586	7242	DB01183	{"V03AB15"}
RxNorm:7243	naltrexone	PA450588	7243	DB00704	{"N07BB04"}
RxNorm:53654	nevirapine	PA450616	53654	DB00238	{"J05AG01"}
RxNorm:7407	nicotine	PA450626	7407	DB00184	{"N07BA01"}
RxNorm:662281	nilotinib	PA165958345	662281	DB04868	{"L01XE08"}
RxNorm:7454	nitrofurantoin	PA450640	7454	DB00698	{"J01XE01"}
RxNorm:7517	norfloxacin	PA450654	7517	DB01059	{"J01MA06","S01AE02"}
RxNorm:7531	nortriptyline	PA450657	7531	DB00540	{"N06AA10"}
RxNorm:61381	olanzapine	PA450688	61381	DB00334	{"N05AH03"}
RxNorm:7646	omeprazole	PA450704	7646	DB00338	{"A02BC01"}
RxNorm:26225	ondansetron	PA450705	26225	DB00904	{"A04AA01"}
RxNorm:32592	oxaliplatin	PA131285527	32592	DB00526	{"L01XA03"}
RxNorm:7781	oxazepam	PA450731	7781	DB00842	{"N05BA04"}
RxNorm:32624	oxcarbazepine	PA450732	32624	DB00776	{"N03AF02"}
RxNorm:7804	oxycodone	PA450741	7804	DB00497	{"N02AA05"}
RxNorm:70561	palonosetron	PA10352	70561	DB00377	{"A04AA05"}
RxNorm:40790	pantoprazole	PA450774	40790	DB00213	{"A02BC02"}
RxNorm:32937	paroxetine	PA450801	32937	DB00715	{"N06AB05"}
RxNorm:714438	pazopanib	PA165291492	714438		{"L01XE"}
RxNorm:120608	peginterferon alfa-2a	PA164749390	120608	DB00008	{"L03AB11","L03AB61"}
RxNorm:253453	peginterferon alfa-2b	PA164784024	253453	DB00022	{"L03AB10","L03AB60"}
RxNorm:1011650	pegloticase	PA165963961	1011650		{"M04AX"}
RxNorm:8076	perphenazine	PA450882	8076	DB00850	{"N05AB03"}
RxNorm:8120	phenazopyridine	PA164746899	8120	DB01438	{"G04BX06"}
RxNorm:8150	phenprocoumon	PA450921	8150	DB00946	{"B01AA04"}
RxNorm:8183	phenytoin	PA450947	8183	DB00252	{"N03AB02","N03AB52"}
RxNorm:8331	pimozide	PA450965	8331	DB01100	{"N05AG02"}
RxNorm:1311280	platinum	PA150595617	1311280		{""}
ATC:L01XA	Platinum compounds	PA164713176			{"L01XA"}
RxNorm:42463	pravastatin	PA451089	42463	DB00175	{"C10AA03"}
RxNorm:8687	primaquine	PA451103	8687	DB01087	{"P01BA03"}
RxNorm:8698	probenecid	PA451106	8698	DB01032	{"M04AB01"}
RxNorm:8754	propafenone	PA451131	8754	DB01182	{"C01BC03"}
RxNorm:8787	propranolol	PA451145	8787	DB00571	{"C07AA05"}
RxNorm:8794	propylthiouracil	PA451156	8794	DB00550	{"H03BA02"}
RxNorm:8886	protriptyline	PA451168	8886	DB00344	{"N06AA11"}
RxNorm:9068	quinidine	PA451209	9068	DB00908	{"C01BA01"}
RxNorm:9071	quinine	PA451213	9071	DB00468	{"P01BC01"}
RxNorm:114979	rabeprazole	PA451216	114979	DB01129	{"A02BC04"}
MedDRA:10037794	radiotherapy	PA166122986			{""}
RxNorm:283821	rasburicase	PA10176	283821	DB00049	{"V03AF07","M04AX01"}
RxNorm:9344	ribavirin	PA451241	9344	DB00811	{"J05AB04"}
RxNorm:35636	risperidone	PA451257	35636	DB00734	{"N05AX08"}
RxNorm:121191	rituximab	PA451261	121191	DB00073	{"L01XC02"}
RxNorm:84108	rosiglitazone	PA451283	84108	DB00412	{"A10BG02"}
RxNorm:301542	rosuvastatin	PA134308647	301542	DB01098	{"C10AA07"}
RxNorm:435	salbutamol	PA448068	435	DB01001	{"R03AC02","R03CC02"}
RxNorm:36117	salmeterol	PA451300	36117	DB00938	{"R03AC12"}
ATC:N06AB	Selective serotonin reuptake inhibitors	PA164713257			{"N06AB"}
RxNorm:36437	sertraline	PA451333	36437	DB01104	{"N06AB06"}
RxNorm:36453	sevoflurane	PA451341	36453	DB01236	{"N01AB08"}
RxNorm:136411	sildenafil	PA451346	136411	DB00203	{"G04BE03"}
RxNorm:36567	simvastatin	PA451363	36567	DB00641	{"C10AA01","C10BA02","C10BX04","A10BH51","C10BA04","C10BX01"}
RxNorm:35302	sirolimus	PA451365	35302	DB00877	{"L04AA10"}
RxNorm:9894	sodium nitrite	PA166115361	9894		{""}
RxNorm:9997	spironolactone	PA451483	9997	DB00421	{"C03DA01"}
RxNorm:10154	succinylcholine	PA451522	10154	DB00202	{"M03AB01"}
RxNorm:10169	sulfacetamide	PA451536	10169	DB00634	{"D10AF06","S01AB04"}
RxNorm:10171	sulfadiazine	PA451539	10171	DB00359	{"J01EC02"}
RxNorm:10831	sulfamethoxazole / trimethoprim	PA10715	10831		{"J01EE"}
RxNorm:9524	sulfasalazine	PA451547	9524	DB00795	{"A07EC01"}
RxNorm:10207	sulfisoxazole	PA164748964	10207	DB00263	{"J01EB05","S01AB02"}
MeSH:D013449	sulfonamides	PA10772			{"A07AB","D06BA","G01AE","S01AB"}
RxNorm:357977	sunitinib	PA162372840	357977	DB01268	{"L01XE04"}
RxNorm:42316	tacrolimus	PA451578	42316	DB00864	{"D11AH01","L04AD02"}
RxNorm:10324	tamoxifen	PA451581	10324	DB00675	{"L02BA01"}
RxNorm:77492	tamsulosin	PA451583	77492	DB00706	{"G04CA02"}
RxNorm:4582	tegafur	PA452620	4582	DB09256	{"L01BC03","L01BC53"}
RxNorm:117466	tenofovir	PA10204	117466	DB00300	{"J05AF"}
RxNorm:37801	terbinafine	PA451614	37801	DB00857	{"D01AE15","D01BA02"}
RxNorm:10390	tetrabenazine	PA140222719	10390	DB04844	{"N07XX06"}
RxNorm:10485	thioguanine	PA451663	10485	DB00352	{"L01BB03"}
RxNorm:10502	thioridazine	PA451666	10502	DB00679	{"N05AC02"}
RxNorm:10600	timolol	PA451690	10600	DB00373	{"C07AA06","S01ED01"}
RxNorm:69120	tiotropium	PA164769056	69120	DB01409	{"R03BB"}
RxNorm:119565	tolterodine	PA164746757	119565	DB01036	{"G04BD07"}
RxNorm:10689	tramadol	PA451735	10689	DB00193	{"N02AX02"}
RxNorm:10759	triamcinolone	PA451749	10759	DB00620	{"A01AC01","C05AA12","D07AB09","D07XB02","H02AB08","R01AD11","R03BA06","S01BA05"}
RxNorm:10834	trimipramine	PA451791	10834	DB00726	{"N06AA06"}
RxNorm:27392	tropisetron	PA161925594	27392	DB11699	{"A04AA03"}
ATC:L04AB	Tumor necrosis factor alpha (TNF-alpha) inhibitors	PA164713366			{"L04AB"}
RxNorm:11118	valproic acid	PA451846	11118	DB00313	{"N03AG01"}
RxNorm:901805	velaglucerase alfa	PA166115366	901805		{""}
RxNorm:39786	venlafaxine	PA451866	39786	DB00285	{"N06AX16"}
RxNorm:1151	vitamin c	PA451898	1151	DB00126	{"G01AD03","S01XA15"}
RxNorm:121243	voriconazole	PA10233	121243	DB00582	{"J02AC03"}
RxNorm:1455099	vortioxetine	PA166122595	1455099		{""}
RxNorm:11289	warfarin	PA451906	11289	DB00682	{"B01AA03"}
RxNorm:114176	zuclopenthixol	PA452629	114176	DB01624	{"N05AF05"}
\.


CREATE TABLE pair
(
  pairid INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  geneSymbol VARCHAR(20) REFERENCES gene(symbol),
  drugId varchar(20) REFERENCES drug(drugId),
  guidelineId INTEGER REFERENCES guideline(id),
  version INTEGER DEFAULT 1,
  level VARCHAR(5) NOT NULL,
  pgkbCALevel VARCHAR(5),
  pgxTesting VARCHAR(50),
  citations TEXT[],

  drugName varchar(100),       -- temporary column, is removed at end of script

  UNIQUE (geneSymbol, drugId)
);

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

copy pair(geneSymbol,drugName,level,pgkbCALevel,pgxTesting,citations) from STDIN;
ABCB1	antidepressants	A/B	3		{}
ABCB1	digoxin	C/D	2A		{}
ABCB1	fentanyl	C/D	2B		{}
ABCB1	methadone	C/D	2B		{}
ABCB1	methotrexate	C/D	2A		{}
ABCB1	morphine	C/D	2B		{}
ABCB1	nevirapine	C/D	2A		{}
ABCB1	ondansetron	C/D	2A		{}
ABCB1	oxycodone	C/D	2B		{}
ABCB1	simvastatin	C/D	2A		{}
ABCB1	sunitinib	C/D	2B		{}
ABCB1	tramadol	C/D	2B		{}
ABCC4	tenofovir	D	2B		{}
ABCG2	rosuvastatin	D	2B		{}
ABL2	valproic acid	B		Actionable PGx	{}
ACE	captopril	D	2A		{}
ADD1	furosemide	D	2B		{}
ADD1	spironolactone	D	2B		{}
ADORA2A	caffeine	D	2B		{}
ADRB2	salbutamol	C/D	2A		{}
ADRB2	salmeterol	C/D	2A		{}
ANKK1	antipsychotics	D	2B		{}
ANKK1	bupropion	D	1B		{}
ANKK1	ethanol	D	2B		{}
APOE	atorvastatin	D	2A		{}
ASL	valproic acid	B		Actionable PGx	{}
ASS1	valproic acid	B		Actionable PGx	{}
ATIC	methotrexate	D	2B		{}
BCHE	succinylcholine	C/D	3	Actionable PGx	{}
C11orf65	metformin	D	2B		{}
C8orf34	irinotecan	D	2B		{}
CACNA1S	desflurane	A	1A	Actionable PGx	{"30499100"}
CACNA1S	enflurane	A	1A	Actionable PGx	{"30499100"}
CACNA1S	halothane	A	1A	Actionable PGx	{"30499100"}
CACNA1S	methoxyflurane	A	1A	Actionable PGx	{"30499100"}
CACNA1S	isoflurane	A	1A	Actionable PGx	{"30499100"}
CACNA1S	sevoflurane	A	1A	Actionable PGx	{"30499100"}
CACNA1S	succinylcholine	A	1A	Actionable PGx	{"30499100"}
CALU	warfarin	D	2B		{}
CBR3	anthracyclines and related substances	D	2B		{}
CCHCR1	nevirapine	D	2B		{}
CES1	clopidogrel	C/D	2B		{}
CETP	hmg coa reductase inhibitors	D	2B		{}
CFTR	ataluren	C	2A		{}
CFTR	ivacaftor	A	1A	Testing required	{"24598717"}
CHRNA3	nicotine	D	2B		{}
COL22A1	salbutamol	D	2B		{}
COMT	Selective serotonin reuptake inhibitors	C	2B		{}
COMT	cisplatin	C/D	3		{}
COMT	nicotine	C/D	2A		{}
COQ2	atorvastatin	D	2B		{}
COQ2	rosuvastatin	D	2B		{}
CPS1	valproic acid	B		Actionable PGx	{}
CRHR1	budesonide	D	2B		{}
CRHR1	fluticasone propionate	D	2B		{}
CRHR1	fluticasone/salmeterol	D	2B		{}
CRHR1	triamcinolone	D	2B		{}
CRHR2	salbutamol	D	2B		{}
CYB5R1	metoclopramide	D		Actionable PGx	{}
CYB5R2	metoclopramide	D		Actionable PGx	{}
CYB5R3	metoclopramide	D		Actionable PGx	{}
CYB5R4	metoclopramide	D		Actionable PGx	{}
CYP2A7P1	efavirenz	C/D			{}
CYP2A7P1	nevirapine	C/D			{}
CYP2B6	efavirenz	B	1B	Actionable PGx	{}
CYP2B6	methadone	B	2A		{}
CYP2B6	nevirapine	B	2A		{}
CYP2C19	amitriptyline	A	1A		{"23486447","27997040"}
CYP2C19	brivaracetam	B/C	4	Actionable PGx	{}
CYP2C19	carisoprodol	B/C	3	Actionable PGx	{}
CYP2C19	citalopram	A	1A	Actionable PGx	{"25974703"}
CYP2C19	clobazam	C	2A	Actionable PGx	{}
CYP2C19	clomipramine	B	2A		{"23486447","27997040"}
CYP2C19	clopidogrel	A	1A	Actionable PGx	{"21716271","23698643"}
CYP2C19	dexlansoprazole	B		Actionable PGx	{}
CYP2C19	diazepam	C	3	Actionable PGx	{}
CYP2C19	doxepin	B	3	Actionable PGx	{"23486447","27997040"}
CYP2C19	escitalopram	A	1A	Actionable PGx	{"25974703"}
CYP2C19	esomeprazole	B	3	Actionable PGx	{}
CYP2C19	flibanserin	C		Actionable PGx	{}
CYP2C19	imipramine	B	2A		{"23486447","27997040"}
CYP2C19	lansoprazole	B	2A	Informative PGx	{}
CYP2C19	omeprazole	B	2A	Actionable PGx	{}
CYP2C19	pantoprazole	B	3	Actionable PGx	{}
CYP2C19	rabeprazole	B	2A	Actionable PGx	{}
CYP2C19	sertraline	B	1A		{"25974703"}
CYP2C19	trimipramine	B	2A		{"23486447","27997040"}
CYP2C19	voriconazole	A	1A	Actionable PGx	{"27981572"}
CYP2C8	rosiglitazone	C/D	2A		{}
CYP2C9	acenocoumarol	B	2A		{}
CYP2C9	celecoxib	B	2A	Actionable PGx	{}
CYP2C9	diclofenac	C	2A		{}
CYP2C9	flibanserin	C		Actionable PGx	{}
CYP2C9	flurbiprofen	B/C	3	Actionable PGx	{}
CYP2C9	lesinurad	C		Actionable PGx	{}
CYP2C9	phenytoin	A	1A	Actionable PGx	{"25099164"}
CYP2C9	warfarin	A	1A	Actionable PGx	{"21900891","28198005"}
CYP2D6	amitriptyline	A	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	aripiprazole	B	3	Actionable PGx	{}
CYP2D6	atomoxetine	A	2A	Actionable PGx	{"30801677"}
CYP2D6	brexpiprazole	B		Actionable PGx	{}
CYP2D6	carvedilol	C	3	Actionable PGx	{}
CYP2D6	cevimeline	C		Actionable PGx	{}
CYP2D6	clomipramine	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	clozapine	C		Actionable PGx	{}
CYP2D6	codeine	A	1A	Actionable PGx	{"22205192","24458010"}
CYP2D6	darifenacin	C		Actionable PGx	{}
CYP2D6	desipramine	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	dextromethorphan	B	3		{}
CYP2D6	dolasetron	C	3		{}
CYP2D6	donepezil	B/C	3	Actionable PGx	{}
CYP2D6	doxepin	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	duloxetine	C		Actionable PGx	{}
CYP2D6	eliglustat	B		Testing required	{}
CYP2D6	fesoterodine	C		Actionable PGx	{}
CYP2D6	flecainide	C	2A		{}
CYP2D6	flibanserin	C		Actionable PGx	{}
CYP2D6	fluoxetine	C	3	Informative PGx	{}
CYP2D6	fluvoxamine	A	1A	Actionable PGx	{"25974703"}
CYP2D6	galantamine	D	3	Informative PGx	{}
CYP2D6	haloperidol	C	3		{}
CYP2D6	iloperidone	B/C	3	Actionable PGx	{}
CYP2D6	imipramine	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	methylphenidate	B	4		{}
CYP2D6	metoprolol	C	2A	Informative PGx	{}
CYP2D6	mirtazapine	B	2A		{}
CYP2D6	modafinil	C		Actionable PGx	{}
CYP2D6	nortriptyline	A	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	olanzapine	C	3		{}
CYP2D6	ondansetron	A	1A	Informative PGx	{"28002639"}
CYP2D6	oxycodone	A	2A		{}
CYP2D6	palonosetron	C		Informative PGx	{}
CYP2D6	paroxetine	A	1A	Informative PGx	{"25974703"}
CYP2D6	perphenazine	B/C		Actionable PGx	{}
CYP2D6	pimozide	B	4	Testing required	{}
CYP2D6	propafenone	C	2A	Actionable PGx	{}
CYP2D6	propranolol	C	4	Informative PGx	{}
CYP2D6	protriptyline	B		Actionable PGx	{}
CYP2D6	quinidine	B		Informative PGx	{}
CYP2D6	quinine	C		Actionable PGx	{}
CYP2D6	risperidone	B	2A	Informative PGx	{}
CYP2D6	sertraline	B	3		{}
CYP2D6	tamoxifen	A	1A	Testing required	{"29385237"}
CYP2D6	tamsulosin	C		Actionable PGx	{}
CYP2D6	terbinafine	C		Informative PGx	{}
CYP2D6	tetrabenazine	C		Testing required	{}
CYP2D6	thioridazine	C	3	Actionable PGx	{}
CYP2D6	timolol	C	3		{}
CYP2D6	tiotropium	C		Informative PGx	{}
CYP2D6	tolterodine	C	2A	Actionable PGx	{}
CYP2D6	tramadol	A	1B	Actionable PGx	{}
CYP2D6	trimipramine	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	tropisetron	A			{"28002639"}
CYP2D6	venlafaxine	B	2A	Informative PGx	{}
CYP2D6	vortioxetine	B	3	Actionable PGx	{}
CYP2D6	zuclopenthixol	C	3		{}
CYP3A4	tacrolimus	C	2A		{}
CYP3A5	atazanavir	C	3		{}
CYP3A5	cyclosporine	C	2B		{}
CYP3A5	midazolam	C	3		{}
CYP3A5	sirolimus	C	2A		{}
CYP3A5	tacrolimus	A	1A		{"25801146"}
CYP4F2	acenocoumarol	B	2B		{}
CYP4F2	phenprocoumon	B	2A		{}
CYP4F2	warfarin	A	1A		{"21900891","28198005"}
DPYD	capecitabine	A	1A	Actionable PGx	{"23988873","29152729"}
DPYD	fluorouracil	A	1A	Actionable PGx	{"23988873","29152729"}
DPYD	tegafur	C	1A		{"23988873"}
DRD2	risperidone	C	2A		{}
DYNC2H1	Platinum compounds	D	2B		{}
DYNC2H1	etoposide	D	2B		{}
EGF	cetuximab	D	2B		{}
EPHX1	carbamazepine	D	2B		{}
ERCC1	Platinum compounds	D	2B		{}
F5	eltrombopag	C		Actionable PGx	{}
F5	hormonal contraceptives for systemic use	C	2A		{}
FCGR3A	cetuximab	D	3		{}
FCGR3A	rituximab	D	2B		{}
FDPS	Bisphosphonates	D	2B		{}
FKBP5	antidepressants	D	2B		{}
FLOT1	carbamazepine	D			{}
G6PD	chloramphenicol	B	3		{}
G6PD	chloroquine	B	3	Actionable PGx	{}
G6PD	chlorpropamide	B		Actionable PGx	{}
G6PD	ciprofloxacin	B	4		{}
G6PD	dabrafenib	B/C		Actionable PGx	{}
G6PD	dapsone	B	1B	Actionable PGx	{}
G6PD	dimercaprol	B	3		{}
G6PD	erythromycin	B			{}
G6PD	glibenclamide	B	3	Actionable PGx	{}
G6PD	glimepiride	B		Actionable PGx	{}
G6PD	glipizide	B		Actionable PGx	{}
G6PD	hydroxychloroquine	B			{}
G6PD	levofloxacin	B			{}
G6PD	lidocaine	B/C			{}
G6PD	mafenide	B		Actionable PGx	{}
G6PD	mefloquine	B	3		{}
G6PD	mesalazine	B			{}
G6PD	methylene blue	B	3	Actionable PGx	{}
G6PD	moxifloxacin	B			{}
G6PD	nalidixic acid	B		Actionable PGx	{}
G6PD	nitrofurantoin	B	3	Actionable PGx	{}
G6PD	norfloxacin	B		Actionable PGx	{}
G6PD	pegloticase	B	3	Testing required	{}
G6PD	phenazopyridine	B	3		{}
G6PD	primaquine	B	3	Testing required	{}
G6PD	probenecid	B		Actionable PGx	{}
G6PD	quinine	B		Actionable PGx	{}
G6PD	rasburicase	A	1A	Testing required	{"24787449"}
G6PD	sodium nitrite	B		Actionable PGx	{}
G6PD	sulfacetamide	B			{}
G6PD	sulfadiazine	B		Actionable PGx	{}
G6PD	sulfamethoxazole / trimethoprim	B	3	Actionable PGx	{}
G6PD	sulfasalazine	B	4	Actionable PGx	{}
G6PD	sulfisoxazole	B			{}
G6PD	vitamin c	C			{}
GBA	velaglucerase alfa	B		Testing required	{}
GGCX	warfarin	D	2B		{}
GNB3	sildenafil	D	2B		{}
GP1BA	aspirin	D	2B		{}
GRIK4	antidepressants	D	2B		{}
GRIK4	citalopram	D			{}
GSTM1	cisplatin	D	2B		{}
GSTM1	oxaliplatin	D	2B		{}
GSTP1	Platinum compounds	C/D	2A		{}
GSTP1	cyclophosphamide	C/D	2A		{}
GSTP1	epirubicin	C/D	2A		{}
GSTP1	fluorouracil	C/D	2A		{}
GSTP1	oxaliplatin	C/D	2A		{}
HAS3	anthracyclines and related substances	D	2B		{}
HLA-A	allopurinol	B	2B		{}
HLA-A	carbamazepine	A	2B	Actionable PGx	{}
HLA-B	abacavir	A	1A	Testing required	{"22378157","24561393"}
HLA-B	allopurinol	A	1A	Actionable PGx	{"23232549","26094938"}
HLA-B	carbamazepine	A	1A	Testing required	{"23695185"}
HLA-B	carbimazole	C	2A		{}
HLA-B	dapsone	C	2A		{}
HLA-B	methazolamide	C	2A		{}
HLA-B	methimazole	C	2A		{}
HLA-B	nevirapine	C	2A		{}
HLA-B	oxcarbazepine	A	1A	Testing recommended	{"23695185"}
HLA-B	phenytoin	A	1A	Testing recommended	{"25099164"}
HLA-B	propylthiouracil	C	2A		{}
HLA-C	allopurinol	C	2B		{}
HLA-C	methazolamide	C	2B		{}
HLA-DPB1	aspirin	C	2B		{}
HLA-DQA1	lapatinib	C	2B	Actionable PGx	{}
HLA-DRB1	nevirapine	C	2B		{}
HMGCR	hmg coa reductase inhibitors	D	2A		{}
HPRT1	mycophenolic acid	B		Actionable PGx	{}
HTR1A	paroxetine	D	2B		{}
HTR2A	antidepressants	D	2B		{}
HTR2A	citalopram	D	2B		{}
HTR2C	clozapine	D	2B		{}
HTR2C	olanzapine	D	2B		{}
HTR2C	risperidone	D	2B		{}
IFNL3	peginterferon alfa-2a	A	1A		{"24096968"}
IFNL3	peginterferon alfa-2b	A	1A	Actionable PGx	{"24096968"}
IFNL3	ribavirin	A	1A		{"24096968"}
IFNL4	peginterferon alfa-2a	D	1A		{}
IFNL4	peginterferon alfa-2b	D	1A		{}
ITPA	interferon alfa-2b, recombinant	C/D	2B		{}
KCNIP4	Ace Inhibitors, Plain	D	3		{}
KIF6	atorvastatin	D	2B		{}
KIF6	pravastatin	D	2B		{}
LDLR	atorvastatin	D		Informative PGx	{}
LPA	hmg coa reductase inhibitors	D	2B		{}
LTC4S	aspirin	D	2B		{}
MC4R	antipsychotics	C	2B		{}
MT-RNR1	aminoglycoside antibacterials	A/B	1B		{}
MTHFR	capecitabine	C	3		{}
MTHFR	carboplatin	C	2A		{}
MTHFR	cyclophosphamide	C	2A		{}
MTHFR	fluorouracil	C	3		{}
MTHFR	leucovorin	C	3		{}
MTHFR	methotrexate	C	2A		{}
MTHFR	oxaliplatin	C	3		{}
MTRR	methotrexate	D	2B		{}
NAGS	carglumic acid	B		Testing required	{}
NAGS	valproic acid	B		Actionable PGx	{}
NAT1	hydralazine	D			{}
NAT1	isosorbide dinitrate	D			{}
NAT2	hydralazine	D	3		{}
NAT2	isoniazid	C	2A		{}
NAT2	isosorbide dinitrate	D			{}
NEDD4L	hydrochlorothiazide	D	2B		{}
NQO1	Alkylating Agents	D	2A		{}
NQO1	Platinum compounds	D	2A		{}
NQO1	anthracyclines and related substances	D	2A		{}
NQO1	fluorouracil	D	2A		{}
NT5C2	gemcitabine	D	2B		{}
NUDT15	azathioprine	A	1B		{"21270794","23422873"}
NUDT15	mercaptopurine	A	1B	Testing recommended	{"21270794","23422873"}
NUDT15	thioguanine	A		Testing recommended	{"21270794","23422873"}
OPRM1	alfentanil	C/D	2B		{}
OPRM1	ethanol	C/D	2B		{}
OPRM1	fentanyl	C/D	2B		{}
OPRM1	methadone	C/D	3		{}
OPRM1	morphine	C/D	2B		{}
OPRM1	naloxone	C/D	2B		{}
OPRM1	naltrexone	C/D	3		{}
OPRM1	tramadol	C/D	2B		{}
OTC	valproic acid	B		Actionable PGx	{}
POLG	divalproex sodium	B		Testing required	{}
POLG	valproic acid	B	3	Testing required	{}
PRKCA	hydrochlorothiazide	D	2B		{}
PROC	warfarin	D		Actionable PGx	{}
PROS1	warfarin	D		Actionable PGx	{}
PTGFR	latanoprost	D	2B		{}
PTGS1	aspirin	D	2B		{}
RYR1	desflurane	A	1A	Actionable PGx	{"30499100"}
RYR1	enflurane	A	1A	Actionable PGx	{"30499100"}
RYR1	halothane	A	1A	Actionable PGx	{"30499100"}
RYR1	methoxyflurane	A	1A	Actionable PGx	{"30499100"}
RYR1	isoflurane	A	1A	Actionable PGx	{"30499100"}
RYR1	sevoflurane	A	1A	Actionable PGx	{"30499100"}
RYR1	succinylcholine	A	1A	Actionable PGx	{"30499100"}
SCN1A	carbamazepine	B	2B		{}
SCN1A	phenytoin	B	2B		{}
SEMA3C	irinotecan	D	2B		{}
SERPINC1	eltrombopag	C		Actionable PGx	{}
SLC28A3	anthracyclines and related substances	D	2B		{}
SLC47A2	metformin	D	3		{}
SLC6A4	citalopram	B/C	2A		{}
SLC6A4	escitalopram	B/C	2A		{}
SLCO1B1	cerivastatin	B	2A		{}
SLCO1B1	methotrexate	C	2A		{}
SLCO1B1	pravastatin	C	2A		{}
SLCO1B1	rosuvastatin	C	2A	Actionable PGx	{}
SLCO1B1	simvastatin	A	1A	Informative PGx	{"22617227","24918167"}
SOD2	cyclophosphamide	D	2B		{}
TANC1	radiotherapy	D	2A		{}
TCF7L2	sulfonamides	D			{}
TMEM43	cisplatin	D			{}
TNF	Tumor necrosis factor alpha (TNF-alpha) inhibitors	C	2B		{}
TP53	cisplatin	D	2B		{}
TP53	cyclophosphamide	D	2B		{}
TPMT	azathioprine	A	1A	Testing recommended	{"21270794","23422873"}
TPMT	mercaptopurine	A	1A	Testing recommended	{"21270794","23422873"}
TPMT	thioguanine	A	1A	Testing recommended	{"21270794","23422873"}
TXNRD2	Selective serotonin reuptake inhibitors	D	2B		{}
TYMS	capecitabine	D	2A		{}
TYMS	fluorouracil	D	2A		{}
UGT1A1	atazanavir	A	1A	Informative PGx	{"26417955"}
UGT1A1	belinostat	B	3	Actionable PGx	{}
UGT1A1	dolutegravir	B/C		Actionable PGx	{}
UGT1A1	irinotecan	A	2A	Actionable PGx	{}
UGT1A1	nilotinib	C	3	Actionable PGx	{}
UGT1A1	pazopanib	B/C	3	Actionable PGx	{}
UGT1A4	lamotrigine	D	2B		{}
UGT2B15	oxazepam	D	2B		{}
UMPS	fluorouracil	D	2B		{}
VDR	peginterferon alfa-2b	D	2A		{}
VDR	ribavirin	D	2A		{}
VKORC1	warfarin	A	1A	Actionable PGx	{"21900891","28198005"}
XPC	cisplatin	C	1B		{}
XRCC1	platinum	D	2B		{}
YEATS4	hydrochlorothiazide	D	2B		{}
\.

-- load drug ID's into pair table and then remove the drug name column
update pair p set drugId=(select drugId from drug d where p.drugName=d.name);
ALTER TABLE pair DROP COLUMN drugName;


CREATE TABLE term (
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  category TEXT NOT NULL,
  term TEXT NOT NULL,
  functionaldef TEXT,
  geneticdef TEXT,
  loinc TEXT,

  UNIQUE (category, term)
);

COMMENT ON TABLE term IS 'Standardized terms for clinical PGx test results';
COMMENT ON COLUMN term.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN term.category IS 'Category group this term falls into';
COMMENT ON COLUMN term.term IS 'The term name';
COMMENT ON COLUMN term.functionaldef IS 'The functional definition of the term';
COMMENT ON COLUMN term.geneticdef IS 'The genetic definition of the term';
COMMENT ON COLUMN term.loinc IS 'The LOINC identifier for the term';

copy term(category,term,functionaldef,geneticdef,loinc) from STDIN;
Allele functional status: all genes	Increased function	Function greater than normal function	N/A	
Allele functional status: all genes	Normal function	Fully functional/wild-type	N/A	
Allele functional status: all genes	Decreased function	Function less than normal function	N/A	
Allele functional status: all genes	No function	Nonfunctional	N/A	
Allele functional status: all genes	Unknown function	No literature describing function or the allele is novel	N/A	
Allele functional status: all genes	Uncertain function	Literature supporting function is conflicting or weak	N/A	
Phenotype: drug-metabolizing enzymes	Ultrarapid metabolizer	Increased enzyme activity compared to rapid metabolizers	Two increased function alleles, or more than 2 normal function alleles	LA10315-2
Phenotype: drug-metabolizing enzymes	Rapid metabolizer	Increased enzyme activity compared to normal metabolizers but less than ultrarapid metabolizers	Combinations of normal function and increased function alleles	LA25390-8
Phenotype: drug-metabolizing enzymes	Normal metabolizer	Fully functional enzyme activity	Combinations of normal function and decreased function alleles	LA25391-6
Phenotype: drug-metabolizing enzymes	Intermediate metabolizer	Decreased enzyme activity (activity between normal and poor metabolizer)	Combinations of normal function, decreased function, and/or no function alleles	LA10317-8
Phenotype: drug-metabolizing enzymes	Poor metabolizer	Little to no enzyme activity	Combination of no function alleles and/ or decreased function alleles	LA9657-3
Phenotype: transporters	Increased function	Increased transporter function compared to normal function. 	One or more increased function alleles	
Phenotype: transporters	Normal function	Fully functional transporter function 	Combinations of normal function and/ or decreased function alleles	
Phenotype: transporters	Decreased function	Decreased transporter function (function between normal and poor function) 	Combinations of normal function, decreased function, and/or no function alleles	
Phenotype: transporters	Poor function	Little to no transporter function	Combination of no function alleles and/ or decreased function alleles	
Phenotype: high-risk genotype status	Positive	Detection of high-risk allele	Homozygous or heterozygous for high-risk allele	LA19541-4
Phenotype: high-risk genotype status	Negative	High-risk allele not detected	No copies of high-risk allele	
\.


CREATE TABLE gene_phenotype
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  geneSymbol VARCHAR(50) REFERENCES gene(symbol) NOT NULL,
  phenotype TEXT,
  ehrPriority TEXT,
  consultationText TEXT,
  activityScore NUMERIC,

  UNIQUE (geneSymbol, phenotype)
);

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
  
  UNIQUE (phenotypeId, diplotype)
);

COMMENT ON TABLE phenotype_diplotype IS 'Specific diplotypes that are associated with a gene phenotype';
COMMENT ON COLUMN phenotype_diplotype.phenotypeId IS 'An ID for a gene_phenotype record, required';
COMMENT ON COLUMN phenotype_diplotype.diplotype IS 'A diplotype for the gene in the form Allele1/Allele2, required';
