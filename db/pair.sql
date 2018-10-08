CREATE TABLE pair
(
  pairid INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  hgncId varchar(20) REFERENCES gene(hgncId),
  drugId varchar(20) REFERENCES drug(drugId),
  guidelineId INTEGER REFERENCES guideline(id),
  version INTEGER DEFAULT 1,
  level VARCHAR(5) NOT NULL,
  pgkbCALevel VARCHAR(5),
  pgxTesting VARCHAR(50),
  citations TEXT[],

  drugName varchar(100),       -- temporary column, is removed at end of script
  pgkbGuidelineId varchar(20), -- temporary column, is removed at end of script
  
  UNIQUE (hgncId, drugId)
);

COMMENT ON TABLE pair IS 'A pair of a gene and a drug that is notable to CPIC';
COMMENT ON COLUMN pair.pairid IS 'A synthetic numerical id, automatically assigned, primary key';
COMMENT ON COLUMN pair.hgncId IS 'The HGNC symbol of the gene in this pair, required';
COMMENT ON COLUMN pair.drugId IS 'The ID of the drug in this pair, required';
COMMENT ON COLUMN pair.guidelineId IS 'The ID of a guideline this pair is described in, optional';
COMMENT ON COLUMN pair.version IS 'The version number, iterates on modification';
COMMENT ON COLUMN pair.level IS 'The CPIC level of this pair, required';
COMMENT ON COLUMN pair.pgkbCALevel IS 'The top level of PharmGKB Clinical Annotation for this pair, optional';
COMMENT ON COLUMN pair.pgxTesting IS 'The top level of PGx testing recommendation from PharmGKB label annotations, optional';
COMMENT ON COLUMN pair.citations IS 'The PMID citations in an array for this pair, optional';

\copy pair(hgncId,drugName,pgkbGuidelineId,level,pgkbCALevel,pgxTesting,citations) from STDIN;
HLA-B	abacavir	PA166104997	A	1A	Testing required	{"22378157","24561393"}
HLA-B	allopurinol	PA166105003	A	1A		{"23232549","26094938"}
CYP2C19	amitriptyline	PA166105006	A	1A		{"23486447","27997040"}
CYP2D6	amitriptyline	PA166105006	A	1A	Actionable PGx	{"23486447","27997040"}
UGT1A1	atazanavir	PA166128738	A	1A		{"26417955"}
TPMT	azathioprine	PA166104933	A	1A	Testing recommended	{"21270794","23422873"}
DPYD	capecitabine	PA166109594	A	1A	Actionable PGx	{"23988873","29152729"}
HLA-A	carbamazepine	PA166153254	A	2B	Actionable PGx	{}
HLA-B	carbamazepine	PA166105008	A	1A	Testing required	{"23695185"}
CYP2C19	clopidogrel	PA166104948	A	1A	Actionable PGx	{"21716271","23698643"}
CYP2D6	codeine	PA166104996	A	1A	Actionable PGx	{"22205192","24458010"}
CACNA1S	desflurane	PA166153253	A	3	Actionable PGx	{}
RYR1	desflurane	PA166153257	A	3	Actionable PGx	{}
DPYD	fluorouracil	PA166122686	A	1A	Actionable PGx	{"23988873","29152729"}
CYP2D6	fluvoxamine	PA166127637	A	1A	Actionable PGx	{"25974703"}
UGT1A1	irinotecan	PA166116092	A	2A	Actionable PGx	{}
CACNA1S	isoflurane	PA166153251	A	3	Actionable PGx	{}
RYR1	isoflurane	PA166153255	A	3	Actionable PGx	{}
CFTR	ivacaftor	PA166114461	A	1A	Testing required	{"24598717"}
TPMT	mercaptopurine	PA166104945	A	1A	Testing recommended	{"21270794","23422873"}
CYP2D6	nortriptyline	PA166104998	A	1A	Actionable PGx	{"23486447","27997040"}
CYP2D6	ondansetron	PA166161954	A	1A	Informative PGx	{"28002639"}
CYP2D6	oxycodone	PA166116088	A	2A		{}
CYP2D6	paroxetine	PA166127636	A	1A	Informative PGx	{"25974703"}
CYP2C9	phenytoin	PA166122806	A	1A	Actionable PGx	{"25099164"}
HLA-B	phenytoin	PA166122806	A	1A	Actionable PGx	{"25099164"}
G6PD	rasburicase	PA166119846	A	1A	Testing required	{"24787449"}
CACNA1S	sevoflurane	PA166153252	A	3	Actionable PGx	{}
RYR1	sevoflurane	PA166153256	A	3	Actionable PGx	{}
SLCO1B1	simvastatin	PA166105005	A	1A		{"22617227","24918167"}
CACNA1S	succinylcholine	PA166153250	A	3	Actionable PGx	{}
CYP3A5	tacrolimus	PA166124619	A	1A		{"25801146"}
CYP2D6	tamoxifen	PA166176068	A	1A		{}
CYP2D6	tamoxifen	PA166116105	A	1A		{}
TPMT	thioguanine	PA166104965	A	1A	Actionable PGx	{"21270794","23422873"}
CYP2D6	tramadol	PA166116089	A	1B	Actionable PGx	{}
CYP2D6	tropisetron	PA166161955	A			{"28002639"}
CYP2C19	voriconazole	PA166161537	A	1A	Actionable PGx	{"27981572"}
CYP2C9	warfarin	PA166104949	A	1A	Actionable PGx	{"21900891","28198005"}
CYP4F2	warfarin	PA166104949	A	1B		{"21900891","28198005"}
VKORC1	warfarin	PA166104949	A	1A	Actionable PGx	{"21900891","28198005"}
CYP2C19	citalopram	PA166127638	A	1A	Actionable PGx	{"25974703"}
CYP2C19	escitalopram	PA166127638	A	1A	Actionable PGx	{"25974703"}
IFNL3	peginterferon alfa-2a	PA166110235	A	1A		{"24096968"}
IFNL3	peginterferon alfa-2b	PA166110235	A	1A	Actionable PGx	{"24096968"}
IFNL3	ribavirin	PA166110235	A	1A		{"24096968"}
CYP2C9	acenocoumarol	PA166153262	B	2A		{}
CYP4F2	acenocoumarol	PA166116133	B	2B		{}
HLA-A	allopurinol	PA166153266	B	2B		{}
CYP2D6	aripiprazole	PA166116129	B	3	Actionable PGx	{}
CYP2D6	atomoxetine	PA166116103	B	2A	Actionable PGx	{}
UGT1A1	belinostat	PA166153268	B	3	Actionable PGx	{}
CYP2D6	brexpiprazole	PA166163798	B		Actionable PGx	{}
SCN1A	carbamazepine	PA166116164	B	2B		{}
NAGS	carglumic acid	PA166153367	B		Testing required	{}
CYP2C9	celecoxib	PA166116132	B	2A	Actionable PGx	{}
SLCO1B1	cerivastatin	PA166163801	B	2A		{}
G6PD	chloramphenicol	PA166116110	B	3		{}
G6PD	chloroquine	PA166116111	B	3	Actionable PGx	{}
G6PD	chlorpropamide	PA166128097	B		Actionable PGx	{}
G6PD	ciprofloxacin	PA166116112	B	4		{}
CYP2C19	clomipramine	PA166105007	B	2A		{"23486447","27997040"}
CYP2D6	clomipramine	PA166105007	B	1A	Actionable PGx	{"23486447","27997040"}
G6PD	dapsone	PA166116113	B	1B	Actionable PGx	{}
CYP2D6	desipramine	PA166105002	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2C19	dexlansoprazole	PA166128100	B		Actionable PGx	{}
G6PD	dimercaprol	PA166116114	B	3		{}
POLG	divalproex sodium	PA166153370	B		Testing required	{}
CYP2C19	doxepin	PA166105000	B	3	Actionable PGx	{"23486447","27997040"}
CYP2D6	doxepin	PA166105000	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2B6	efavirenz	PA166116170	B	1B	Actionable PGx	{}
CYP2D6	eliglustat	PA166153263	B		Testing required	{}
CYP2C19	esomeprazole	PA166116093	B	3	Actionable PGx	{}
G6PD	glibenclamide	PA166131625	B	3	Actionable PGx	{}
G6PD	glimepiride	PA166128103	B		Actionable PGx	{}
G6PD	glipizide	PA166128104	B		Actionable PGx	{}
G6PD	hydroxychloroquine	PA166116115	B			{}
CYP2C19	imipramine	PA166104999	B	2A		{"23486447","27997040"}
CYP2D6	imipramine	PA166104999	B	1A	Actionable PGx	{"23486447","27997040"}
CYP2C19	lansoprazole	PA166116094	B	2A	Informative PGx	{}
G6PD	levofloxacin	PA166116116	B			{}
G6PD	mafenide	PA166128108	B		Actionable PGx	{}
G6PD	mefloquine	PA166116117	B	3		{}
G6PD	mesalazine	PA166116118	B			{}
CYP2B6	methadone	PA166153261	B	2A		{}
G6PD	methylene blue	PA166116119	B	3	Actionable PGx	{}
CYP2D6	methylphenidate	PA166116104	B	4		{}
CYP2D6	mirtazapine	PA166116101	B	2A		{}
G6PD	moxifloxacin	PA166116120	B			{}
HPRT1	mycophenolic acid	PA166153364	B		Actionable PGx	{}
G6PD	nalidixic acid	PA166128110	B		Actionable PGx	{}
CYP2B6	nevirapine	PA166116178	B	2A		{}
G6PD	nitrofurantoin	PA166116121	B	3	Actionable PGx	{}
G6PD	norfloxacin	PA166116122	B		Actionable PGx	{}
CYP2C19	omeprazole	PA166116095	B	2A	Actionable PGx	{}
HLA-B	oxcarbazepine	PA166176623	A	1A	Testing recommended	{"23695185"}
HLA-B	oxcarbazepine	PA166153267	B	3	Testing recommended	{}
CYP2C19	pantoprazole	PA166128111	B	3	Actionable PGx	{}
G6PD	pegloticase	PA166128113	B	3	Testing required	{}
G6PD	phenazopyridine	PA166116123	B	3		{}
CYP4F2	phenprocoumon	PA166116134	B	2A		{}
SCN1A	phenytoin	PA166116181	B	2B		{}
CYP2D6	pimozide	PA166128115	B	4	Testing required	{}
G6PD	primaquine	PA166116124	B	3	Testing required	{}
G6PD	probenecid	PA166116125	B		Actionable PGx	{}
CYP2D6	protriptyline	PA166116085	B		Actionable PGx	{}
G6PD	quinine	PA166128116	B		Actionable PGx	{}
CYP2C19	rabeprazole	PA166116096	B	2A	Actionable PGx	{}
CYP2D6	risperidone	PA166116130	B	2A	Informative PGx	{}
CYP2C19	sertraline	PA166127639	B	1A		{"25974703"}
CYP2D6	sertraline	PA166116108	B	3		{}
G6PD	sodium nitrite	PA166128117	B		Actionable PGx	{}
RYR1	succinylcholine	PA166116131	B	3	Actionable PGx	{}
G6PD	sulfacetamide	PA166116126	B			{}
G6PD	sulfadiazine	PA166153264	B		Actionable PGx	{}
G6PD	sulfamethoxazole / trimethoprim	PA166116127	B	3	Actionable PGx	{}
G6PD	sulfasalazine	PA166116128	B	4	Actionable PGx	{}
CYP2C19	trimipramine	PA166105001	B	2A		{"23486447","27997040"}
CYP2D6	trimipramine	PA166105001	B	1A	Actionable PGx	{"23486447","27997040"}
ABL2	valproic acid	PA166153358	B		Actionable PGx	{}
ASL	valproic acid	PA166153359	B		Actionable PGx	{}
ASS1	valproic acid	PA166153360	B		Actionable PGx	{}
CPS1	valproic acid	PA166153361	B		Actionable PGx	{}
NAGS	valproic acid	PA166153366	B		Actionable PGx	{}
OTC	valproic acid	PA166153368	B		Actionable PGx	{}
POLG	valproic acid	PA166153369	B	3	Testing required	{}
GBA	velaglucerase alfa	PA166153363	B		Testing required	{}
CYP2D6	venlafaxine	PA166116102	B	2A	Informative PGx	{}
CYP2D6	vortioxetine	PA166128118	B	3	Actionable PGx	{}
CYP4F2	warfarin	PA166116135	B	1B		{}
CYP2D6	dextromethorphan	PA166153249	B	3		{}
CYP2D6	quinidine	PA166153249	B		Informative PGx	{}
G6PD	erythromycin	PA166153265	B			{}
G6PD	sulfisoxazole	PA166153265	B			{}
CYP2C19	brivaracetam	PA166163799	B/C	4	Actionable PGx	{}
CYP2C19	carisoprodol	PA166128096	B/C	3	Actionable PGx	{}
SLC6A4	citalopram	PA166153270	B/C	2A		{}
G6PD	dabrafenib	PA166128099	B/C		Actionable PGx	{}
UGT1A1	dolutegravir	PA166163802	B/C		Actionable PGx	{}
CYP2D6	donepezil	PA166163803	B/C	3	Actionable PGx	{}
SLC6A4	escitalopram	PA166153269	B/C	2A		{}
CYP2C9	flurbiprofen	PA166128101	B/C	3	Actionable PGx	{}
CYP2D6	iloperidone	PA166128106	B/C	3	Actionable PGx	{}
G6PD	lidocaine	PA166163810	B/C			{}
UGT1A1	pazopanib	PA166128112	B/C	3	Actionable PGx	{}
CYP2D6	perphenazine	PA166128114	B/C		Actionable PGx	{}
HLA-C	allopurinol	PA166153288	C	2B		{}
MT-RNR1	aminoglycoside antibacterials	PA166153294	C	1B		{}
MC4R	antipsychotics	PA166153293	C	2B		{}
HLA-DPB1	aspirin	PA166153290	C	2B		{}
CFTR	ataluren	PA166153271	C	2A		{}
CYP3A5	atazanavir	PA166153279	C	3		{}
MTHFR	capecitabine	PA166116155	C	3		{}
HLA-B	carbimazole	PA166153285	C	2A		{}
MTHFR	carboplatin	PA166116156	C	2A		{}
CYP2D6	carvedilol	PA166116149	C	3	Actionable PGx	{}
CYP2D6	cevimeline	PA166116142	C		Actionable PGx	{}
XPC	cisplatin	PA166153297	C	1B		{}
CYP2C19	clobazam	PA166128098	C	2A	Actionable PGx	{}
CYP2D6	clozapine	PA166116137	C		Actionable PGx	{}
MTHFR	cyclophosphamide	PA166116157	C	2A		{}
CYP3A5	cyclosporine	PA166153278	C	2B		{}
HLA-B	dapsone	PA166153282	C	2A		{}
CYP2D6	darifenacin	PA166153274	C		Actionable PGx	{}
CYP2C19	diazepam	PA166116136	C	3	Actionable PGx	{}
CYP2C9	diclofenac	PA166153272	C	2A		{}
CYP2D6	dolasetron	PA166161899	C	3		{}
CYP2D6	duloxetine	PA166163804	C		Actionable PGx	{}
F5	eltrombopag	PA166153280	C		Actionable PGx	{}
SERPINC1	eltrombopag	PA166153295	C		Actionable PGx	{}
CYP2D6	fesoterodine	PA166153275	C		Actionable PGx	{}
CYP2D6	flecainide	PA166116143	C	2A		{}
CYP2C19	flibanserin	PA166163806	C		Actionable PGx	{}
CYP2C9	flibanserin	PA166163805	C		Actionable PGx	{}
CYP2D6	flibanserin	PA166163807	C		Actionable PGx	{}
MTHFR	fluorouracil	PA166116158	C	3		{}
CYP2D6	fluoxetine	PA166116106	C	3	Informative PGx	{}
CYP2D6	haloperidol	PA166116138	C	3		{}
F5	hormonal contraceptives for systemic use	PA166116153	C	2A		{}
NAT2	isoniazid	PA166116162	C	2A		{}
HLA-DQA1	lapatinib	PA166153291	C	2B	Actionable PGx	{}
CYP2C9	lesinurad	PA166163809	C		Actionable PGx	{}
MTHFR	leucovorin	PA166116159	C	3		{}
HLA-B	methazolamide	PA166153284	C	2A		{}
HLA-C	methazolamide	PA166153289	C	2B		{}
HLA-B	methimazole	PA166153286	C	2A		{}
MTHFR	methotrexate	PA166116160	C	2A		{}
SLCO1B1	methotrexate	PA166163811	C	2A		{}
CYP2D6	metoprolol	PA166116150	C	2A	Informative PGx	{}
CYP3A5	midazolam	PA166163812	C	3		{}
CYP2D6	modafinil	PA166153273	C		Actionable PGx	{}
HLA-B	nevirapine	PA166153283	C	2A		{}
HLA-DRB1	nevirapine	PA166153292	C	2B		{}
UGT1A1	nilotinib	PA166116154	C	3	Actionable PGx	{}
CYP2D6	olanzapine	PA166116139	C	3		{}
MTHFR	oxaliplatin	PA166116161	C	3		{}
CYP2D6	palonosetron	PA166161901	C		Informative PGx	{}
SLCO1B1	pravastatin	PA166163814	C	2A		{}
CYP2D6	propafenone	PA166116144	C	2A	Actionable PGx	{}
CYP2D6	propranolol	PA166116151	C	4	Informative PGx	{}
HLA-B	propylthiouracil	PA166153287	C	2A		{}
CYP2D6	quinine	PA166153362	C		Actionable PGx	{}
DRD2	risperidone	PA166116163	C	2A		{}
SLCO1B1	rosuvastatin	PA166163817	C	2A	Actionable PGx	{}
COMT	Selective serotonin reuptake inhibitors	PA166163818	C	2B		{}
CYP3A5	sirolimus	PA166153277	C	2A		{}
CYP3A4	tacrolimus	PA166153276	C	2A		{}
CYP2D6	tamsulosin	PA166163819	C		Actionable PGx	{}
DPYD	tegafur	PA166122687	C	1A		{"23988873"}
CYP2D6	terbinafine	PA166116145	C		Informative PGx	{}
CYP2D6	tetrabenazine	PA166116146	C		Testing required	{}
CYP2D6	thioridazine	PA166116140	C	3	Actionable PGx	{}
CYP2D6	timolol	PA166116152	C	3		{}
CYP2D6	tiotropium	PA166116147	C		Informative PGx	{}
CYP2D6	tolterodine	PA166116148	C	2A	Actionable PGx	{}
TNF	Tumor necrosis factor alpha (TNF-alpha) inhibitors	PA166153296	C	2B		{}
G6PD	vitamin c	PA166153281	C			{}
CYP2D6	zuclopenthixol	PA166116141	C	3		{}
OPRM1	alfentanil	PA166153309	C/D	2B		{}
COMT	cisplatin	PA166116165	C/D	3		{}
CES1	clopidogrel	PA166116166	C/D	2B		{}
GSTP1	cyclophosphamide	PA166116167	C/D	2A		{}
ABCB1	digoxin	PA166116168	C/D	2A		{}
CYP2A7P1	efavirenz	PA166116169	C/D			{}
GSTP1	epirubicin	PA166116171	C/D	2A		{}
OPRM1	ethanol	PA166153308	C/D	2B		{}
ABCB1	fentanyl	PA166153301	C/D	2B		{}
OPRM1	fentanyl	PA166153310	C/D	2B		{}
GSTP1	fluorouracil	PA166116172	C/D	2A		{}
ITPA	interferon alfa-2b, recombinant	PA166116173	C/D	2B		{}
ABCB1	methadone	PA166153302	C/D	2B		{}
OPRM1	methadone	PA166153312	C/D	3		{}
ABCB1	methotrexate	PA166153300	C/D	2A		{}
ABCB1	morphine	PA166153303	C/D	2B		{}
OPRM1	morphine	PA166116174	C/D	2B		{}
OPRM1	naloxone	PA166116175	C/D	2B		{}
OPRM1	naltrexone	PA166153311	C/D	3		{}
ABCB1	nevirapine	PA166116176	C/D	2A		{}
CYP2A7P1	nevirapine	PA166116177	C/D			{}
COMT	nicotine	PA166116179	C/D	2A		{}
ABCB1	ondansetron	PA166153299	C/D	2A		{}
GSTP1	oxaliplatin	PA166116180	C/D	2A		{}
ABCB1	oxycodone	PA166153304	C/D	2B		{}
GSTP1	Platinum compounds	PA166116182	C/D	2A		{}
CYP2C8	rosiglitazone	PA166163816	C/D	2A		{}
ADRB2	salbutamol	PA166116183	C/D	2A		{}
ADRB2	salmeterol	PA166116184	C/D	2A		{}
ABCB1	simvastatin	PA166153298	C/D	2A		{}
BCHE	succinylcholine	PA166153307	C/D	3	Actionable PGx	{}
ABCB1	sunitinib	PA166153306	C/D	2B		{}
ABCB1	tramadol	PA166153305	C/D	2B		{}
OPRM1	tramadol	PA166153313	C/D	2B		{}
KCNIP4	Ace Inhibitors, Plain	PA166153248	D	3		{}
NQO1	Alkylating Agents	PA166153342	D	2A		{}
CBR3	anthracyclines and related substances	PA166116185	D	2B		{}
HAS3	anthracyclines and related substances	PA166153334	D	2B		{}
NQO1	anthracyclines and related substances	PA166153343	D	2A		{}
SLC28A3	anthracyclines and related substances	PA166163797	D	2B		{}
FKBP5	antidepressants	PA166153329	D	2B		{}
GRIK4	antidepressants	PA166153331	D	2B		{}
HTR2A	antidepressants	PA166153336	D	2B		{}
ANKK1	antipsychotics	PA166116186	D	2B		{}
GP1BA	aspirin	PA166153330	D	2B		{}
LTC4S	aspirin	PA166116187	D	2B		{}
PTGS1	aspirin	PA166153351	D	2B		{}
APOE	atorvastatin	PA166153316	D	2A		{}
COQ2	atorvastatin	PA166116188	D	2B		{}
KIF6	atorvastatin	PA166153339	D	2B		{}
LDLR	atorvastatin	PA166153365	D		Informative PGx	{}
FDPS	Bisphosphonates	PA166116189	D	2B		{}
CRHR1	budesonide	PA166116190	D	2B		{}
ANKK1	bupropion	PA166116191	D	1B		{}
ADORA2A	caffeine	PA166116192	D	2B		{}
TYMS	capecitabine	PA166163800	D	2A		{}
ACE	captopril	PA166153314	D	2A		{}
EPHX1	carbamazepine	PA166116194	D	2B		{}
FLOT1	carbamazepine	PA166116193	D			{}
EGF	cetuximab	PA166116196	D	2B		{}
FCGR3A	cetuximab	PA166116195	D	3		{}
GSTM1	cisplatin	PA166153332	D	2B		{}
TMEM43	cisplatin	PA166116197	D			{}
TP53	cisplatin	PA166116198	D	2B		{}
GRIK4	citalopram	PA166116199	D			{}
HTR2A	citalopram	PA166116200	D	2B		{}
HTR2C	clozapine	PA166116201	D	2B		{}
SOD2	cyclophosphamide	PA166116202	D	2B		{}
TP53	cyclophosphamide	PA166116203	D	2B		{}
ANKK1	ethanol	PA166153315	D	2B		{}
DYNC2H1	etoposide	PA166153327	D	2B		{}
NQO1	fluorouracil	PA166153344	D	2A		{}
TYMS	fluorouracil	PA166163808	D	2A		{}
UMPS	fluorouracil	PA166116204	D	2B		{}
CRHR1	fluticasone propionate	PA166153321	D	2B		{}
CRHR1	fluticasone/salmeterol	PA166153322	D	2B		{}
ADD1	furosemide	PA166116205	D	2B		{}
CYP2D6	galantamine	PA166128102	D	3	Informative PGx	{}
NT5C2	gemcitabine	PA166153346	D	2B		{}
CETP	hmg coa reductase inhibitors	PA166153319	D	2B		{}
HMGCR	hmg coa reductase inhibitors	PA166116222	D	2A		{}
LPA	hmg coa reductase inhibitors	PA166153340	D	2B		{}
NEDD4L	hydrochlorothiazide	PA166153341	D	2B		{}
PRKCA	hydrochlorothiazide	PA166153347	D	2B		{}
YEATS4	hydrochlorothiazide	PA166116206	D	2B		{}
C8orf34	irinotecan	PA166153317	D	2B		{}
SEMA3C	irinotecan	PA166153352	D	2B		{}
UGT1A4	lamotrigine	PA166153357	D	2B		{}
PTGFR	latanoprost	PA166153350	D	2B		{}
C11orf65	metformin	PA166116207	D	2B		{}
SLC47A2	metformin	PA166153353	D	3		{}
ATIC	methotrexate	PA166116209	D	2B		{}
MTRR	methotrexate	PA166116208	D	2B		{}
CYB5R1	metoclopramide	PA166128186	D		Actionable PGx	{}
CYB5R2	metoclopramide	PA166153324	D		Actionable PGx	{}
CYB5R3	metoclopramide	PA166153325	D		Actionable PGx	{}
CYB5R4	metoclopramide	PA166153326	D		Actionable PGx	{}
CCHCR1	nevirapine	PA166153318	D	2B		{}
CHRNA3	nicotine	PA166153320	D	2B		{}
HTR2C	olanzapine	PA166116210	D	2B		{}
GSTM1	oxaliplatin	PA166153333	D	2B		{}
UGT2B15	oxazepam	PA166116211	D	2B		{}
HTR1A	paroxetine	PA166153335	D	2B		{}
VDR	peginterferon alfa-2b	PA166163813	D	2A		{}
XRCC1	platinum	PA166116213	D	2B		{}
DYNC2H1	Platinum compounds	PA166153328	D	2B		{}
ERCC1	Platinum compounds	PA166116212	D	2B		{}
NQO1	Platinum compounds	PA166153345	D	2A		{}
KIF6	pravastatin	PA166153338	D	2B		{}
TANC1	radiotherapy	PA166153354	D	2A		{}
VDR	ribavirin	PA166163815	D	2A		{}
HTR2C	risperidone	PA166116214	D	2B		{}
FCGR3A	rituximab	PA166116215	D	2B		{}
ABCG2	rosuvastatin	PA166116216	D	2B		{}
COQ2	rosuvastatin	PA166116217	D	2B		{}
COL22A1	salbutamol	PA166116218	D	2B		{}
CRHR2	salbutamol	PA166116219	D	2B		{}
TXNRD2	Selective serotonin reuptake inhibitors	PA166153356	D	2B		{}
GNB3	sildenafil	PA166116220	D	2B		{}
ADD1	spironolactone	PA166116221	D	2B		{}
TCF7L2	sulfonamides	PA166153355	D			{}
ABCC4	tenofovir	PA166116223	D	2B		{}
CRHR1	triamcinolone	PA166153323	D	2B		{}
CALU	warfarin	PA166116225	D	2B		{}
GGCX	warfarin	PA166116224	D	2B		{}
PROC	warfarin	PA166153348	D		Actionable PGx	{}
PROS1	warfarin	PA166153349	D		Actionable PGx	{}
NAT1	hydralazine	PA166128107	D			{}
NAT2	hydralazine	PA166128107	D	3		{}
NAT1	isosorbide dinitrate	PA166128107	D			{}
NAT2	isosorbide dinitrate	PA166128107	D			{}
IFNL4	peginterferon alfa-2a	PA166153337	D	1A		{}
IFNL4	peginterferon alfa-2b	PA166153337	D	1A		{}
ABCB1	antidepressants	PA166153258	A/B	3		{}
NUDT15	azathioprine	PA166153259	A/B	1B		{}
NUDT15	mercaptopurine	PA166153260	A/B	1B		{}
\.

-- load drug ID's into pair table and then remove the drug name column
update pair p set drugId=(select drugId from drug d where p.drugName=d.name);
update pair p set guidelineId=(select id from guideline g where p.pgkbGuidelineId=g.pharmgkbId);
ALTER TABLE pair DROP COLUMN drugName;
ALTER TABLE pair DROP COLUMN pgkbGuidelineId;
