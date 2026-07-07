-- these can stick around
alter function cpic.pharmgkb_guideline_alleles(text) rename to clinpgx_guideline_alleles;
alter function cpic.pharmgkb_guideline_recommendation(text, text) rename to clinpgx_guideline_recommendation;

-- this is not used in API and is replaced by clinpgx_guideline_recommendation
drop function cpic.pharmgkb_recommendation;
