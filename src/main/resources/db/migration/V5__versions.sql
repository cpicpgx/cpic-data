create or replace function increment_version()
returns trigger
as
  $body$
  begin
    new.version := new.version + 1;
    return new;
  end;
  $body$
language plpgsql;

create trigger version_guideline
  before update on guideline
  for each row execute procedure increment_version();
create trigger version_allele
  before update on allele
  for each row execute procedure increment_version();
create trigger version_sequence_location
  before update on sequence_location
  for each row execute procedure increment_version();
create trigger version_pair
  before update on pair
  for each row execute procedure increment_version();

alter table publication add column version integer default 1;
create trigger version_publication
  before update on publication
  for each row execute procedure increment_version();

alter table gene add column version integer default 1;
create trigger version_gene
  before update on gene
  for each row execute procedure increment_version();

alter table allele_location_value add column version integer default 1;
create trigger version_pallele_location_value
  before update on allele_location_value
  for each row execute procedure increment_version();

alter table translation_note add column version integer default 1;
create trigger version_translation_note
  before update on translation_note
  for each row execute procedure increment_version();

alter table population add column version integer default 1;
create trigger version_population
  before update on population
  for each row execute procedure increment_version();

alter table allele_frequency add column version integer default 1;
create trigger version_allele_frequency
  before update on allele_frequency
  for each row execute procedure increment_version();

alter table function_reference add column version integer default 1;
create trigger version_function_reference
  before update on function_reference
  for each row execute procedure increment_version();

alter table drug add column version integer default 1;
create trigger version_drug
  before update on drug
  for each row execute procedure increment_version();

alter table term add column version integer default 1;
create trigger version_term
  before update on term
  for each row execute procedure increment_version();

alter table gene_phenotype add column version integer default 1;
create trigger version_gene_phenotype
  before update on gene_phenotype
  for each row execute procedure increment_version();

alter table phenotype_diplotype add column version integer default 1;
create trigger version_phenotype_diplotype
  before update on phenotype_diplotype
  for each row execute procedure increment_version();

alter table recommendation add column version integer default 1;
create trigger version_recommendation
  before update on recommendation
  for each row execute procedure increment_version();

alter table test_alerts add column version integer default 1;
create trigger version_test_alerts
  before update on test_alerts
  for each row execute procedure increment_version();
