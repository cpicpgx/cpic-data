ALTER TABLE allele ADD COLUMN frequency JSONB;
COMMENT ON COLUMN allele.frequency IS 'JSON map of frequencies for this allele with keys being biogeographical groups';

ALTER TABLE gene_result ADD COLUMN frequency JSONB;
COMMENT ON COLUMN gene_result.frequency IS 'JSON map of frequencies for this gene result with keys being biogeographical groups';

ALTER TABLE gene_result_diplotype ADD COLUMN frequency JSONB;
COMMENT ON COLUMN allele.frequency IS 'JSON map of frequencies for this diplotype with keys being biogeographical groups';
