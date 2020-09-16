alter table pair add constraint valid_pmids CHECK ( array_to_string(citations, '') ~ '\d+' );
