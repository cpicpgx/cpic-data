update gene set notesondiplotype='CYP2C9 diplotype to phenotype translation is not applicable to the warfarin guideline.'
where symbol='CYP2C9';
update gene set notesondiplotype='CYP4F2 diplotype to phenotype translation is not applicable to the warfarin guideline.'
where symbol='CYP4F2';
update gene set notesondiplotype='VKORC1 diplotype to phenotype translation is not applicable to the warfarin guideline.'
where symbol='VKORC1';

alter table guideline add notesonusage TEXT;
comment on column guideline.notesonusage is 'Notes about using a particular guideline within this system. This may include. You may find information here when data for a guideline seems missing or incomplete.';

update guideline set notesonusage='Warfarin recommendation does not follow simple diplotype to phenotype translation. Read the guideline text and follow the recommendation diagram found at http://files.cpicpgx.org/images/warfarin/warfarin_recommendation_diagram.png' where name ~ 'Warfarin';
