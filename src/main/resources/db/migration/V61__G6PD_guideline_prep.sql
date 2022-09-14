-- rename the reference sequence allele for G6PD
update allele_definition set name='B (reference)' where genesymbol='G6PD' and name='B (wildtype)';
update allele set name='B (reference)' where genesymbol='G6PD' and name='B (wildtype)';

-- switch the name of these drugs since that's how it's referred in the new guideline
update drug set name='glyburide' where name='glibenclamide';
update drug set name='mepacrine' where name='quinacrine';
update drug set name='nitrofural' where name='nitrofurazone';
