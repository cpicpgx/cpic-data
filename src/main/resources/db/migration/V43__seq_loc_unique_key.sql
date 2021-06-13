alter table sequence_location add constraint chromosomeLocation_nn check ( chromosomelocation is not null );
alter table sequence_location add constraint chromosomelocation_unique unique ( chromosomelocation );
