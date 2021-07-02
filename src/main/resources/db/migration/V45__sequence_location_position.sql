alter table sequence_location add position integer;
comment on column sequence_location.position is 'The position, or starting position, of this variation on the chromosomal/mitochondrial sequence';

update sequence_location set position=to_number((regexp_match(chromosomelocation, '\d+'))[1], '9999999999999')
where chromosomelocation is not null;

alter table cpic.sequence_location add constraint position_nn check (position is not null);
