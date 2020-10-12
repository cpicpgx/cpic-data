ALTER TABLE allele_definition ADD structuralvariation BOOLEAN DEFAULT FALSE;
COMMENT ON COLUMN allele_definition.structuralvariation IS 'Indicates whether this allele definition is a structural variant, default is false. If true, look in the pharmvarid field for the PharmVar ID that defines the structural variation.';
