CREATE TABLE terms (
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  category VARCHAR(200),
  term VARCHAR(200),
  functionaldef VARCHAR(200),
  geneticdef VARCHAR(200)
);

\copy terms(category,term,functionaldef,geneticdef) from STDIN;
Allele functional status: all genes	Increased function	Function greater than normal function	N/A
Allele functional status: all genes	Normal function	Fully functional/wild-type	N/A
Allele functional status: all genes	Decreased function	Function less than normal function	N/A
Allele functional status: all genes	No function	Nonfunctional	N/A
Allele functional status: all genes	Unknown function	No literature describing function or the allele is novel	N/A
Allele functional status: all genes	Uncertain function	Literature supporting function is conflicting or weak	N/A
Phenotype: drug-metabolizing enzymes	Ultrarapid metabolizer	Increased enzyme activity compared to rapid metabolizers	Two increased function alleles, or more than 2 normal function alleles
Phenotype: drug-metabolizing enzymes	Rapid metabolizer	Increased enzyme activity compared to normal metabolizers but less than ultrarapid metabolizers	Combinations of normal function and increased function alleles
Phenotype: drug-metabolizing enzymes	Normal metabolizer	Fully functional enzyme activity	Combinations of normal function and decreased function alleles
Phenotype: drug-metabolizing enzymes	Intermediate metabolizer	Decreased enzyme activity (activity between normal and poor metabolizer)	Combinations of normal function, decreased function, and/or no function alleles
Phenotype: drug-metabolizing enzymes	Poor metabolizer	Little to no enzyme activity	Combination of no function alleles and/ or decreased function alleles
Phenotype: transporters	Increased function	Increased transporter function compared to normal function. 	One or more increased function alleles
Phenotype: transporters	Normal function	Fully functional transporter function 	Combinations of normal function and/ or decreased function alleles
Phenotype: transporters	Decreased function	Decreased transporter function (function between normal and poor function) 	Combinations of normal function, decreased function, and/or no function alleles
Phenotype: transporters	Poor function	Little to no transporter function	Combination of no function alleles and/ or decreased function alleles
Phenotype: high-risk genotype status	Positive	Detection of high-risk allele	Homozygous or heterozygous for high-risk allele
Phenotype: high-risk genotype status	Negative	High-risk allele not detected	No copies of high-risk allele
\.
