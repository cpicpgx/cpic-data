alter table allele add findings_text text;

update allele set findings_text=findings::text where findings is not null;

alter table allele drop column findings;

alter table allele rename column findings_text to findings;