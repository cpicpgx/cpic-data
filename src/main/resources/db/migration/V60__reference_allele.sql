ALTER TABLE allele ADD COLUMN inferredFrequency BOOL DEFAULT FALSE;

COMMENT ON COLUMN allele.inferredFrequency IS 'Indicates (with "true") that the frequencies for this allele have been inferred by an algorithm that aggregates the frequencies for other alleles of this gene and is not based on direct data, defaults to false, should only be 1 per gene';

ALTER TABLE allele_definition RENAME COLUMN reference TO matchesReferenceSequence;

COMMENT ON COLUMN allele_definition.matchesReferenceSequence IS 'Indicates (with "true") the variants that define the named allele will match the reference sequence for the gene, defaults to false, should only be 1 per gene';
