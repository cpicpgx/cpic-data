CREATE TABLE recommendation 
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  guidelineId INTEGER REFERENCES guideline(id),
  drugId VARCHAR(20) REFERENCES drug(drugId),
  implications TEXT,
  drug_recommendation TEXT,
  classification VARCHAR(20),
  genotypes JSONB
);

COMMENT ON TABLE recommendation IS 'Recommendations for a gene phenotype pulled from a guideline';
COMMENT ON COLUMN recommendation.id IS 'A synthetic numerical ID, auto-assigned, primary key';
COMMENT ON COLUMN recommendation.guidelineId IS 'The guideline this recommendation appears in';
COMMENT ON COLUMN recommendation.drugId IS 'The drug this recommendation is for';
COMMENT ON COLUMN recommendation.implications IS 'Implications for phenotypic measures';
COMMENT ON COLUMN recommendation.drug_recommendation IS 'Dosing or therapeutic recommendations, depending on particular drug';
COMMENT ON COLUMN recommendation.classification IS 'Classification of recommendations, described in supplementary meterial';
COMMENT ON COLUMN recommendation.genotypes IS 'Genotypes that this recommendation applies to, this is a JSON Array';
