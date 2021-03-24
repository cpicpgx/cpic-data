/**
  Add a new "url" field to the gene data model so we can link back to the CPIC website in certain queries
 */

alter table gene add url text;

update gene set url='https://cpicpgx.org/gene/oprm1/' where symbol=upper('oprm1');
update gene set url='https://cpicpgx.org/gene/comt/' where symbol=upper('comt');
update gene set url='https://cpicpgx.org/gene/ifnl/' where symbol=upper('ifnl3');
update gene set url='https://cpicpgx.org/gene/ifnl/' where symbol=upper('ifnl4');
update gene set url='https://cpicpgx.org/gene/ugt1a1/' where symbol=upper('ugt1a1');
update gene set url='https://cpicpgx.org/gene/nudt15/' where symbol=upper('nudt15');
update gene set url='https://cpicpgx.org/gene/tpmt/' where symbol=upper('tpmt');
update gene set url='https://cpicpgx.org/gene/slco1b1/' where symbol=upper('slco1b1');
update gene set url='https://cpicpgx.org/gene/cacna1s/' where symbol=upper('cacna1s');
update gene set url='https://cpicpgx.org/gene/ryr1/' where symbol=upper('ryr1');
update gene set url='https://cpicpgx.org/gene/g6pd/' where symbol=upper('g6pd');
update gene set url='https://cpicpgx.org/gene/dpyd/' where symbol=upper('dpyd');
update gene set url='https://cpicpgx.org/gene/cyp3a5/' where symbol=upper('cyp3a5');
update gene set url='https://cpicpgx.org/gene/cyp4f2/' where symbol=upper('cyp4f2');
update gene set url='https://cpicpgx.org/gene/vkorc1/' where symbol=upper('vkorc1');
update gene set url='https://cpicpgx.org/gene/cyp2b6/' where symbol=upper('cyp2b6');
update gene set url='https://cpicpgx.org/gene/cftr/' where symbol=upper('cftr');
update gene set url='https://cpicpgx.org/gene/hla/' where symbol=upper('hla-a');
update gene set url='https://cpicpgx.org/gene/hla/' where symbol=upper('hla-b');
update gene set url='https://cpicpgx.org/gene/cyp2d6/' where symbol=upper('cyp2d6');
update gene set url='https://cpicpgx.org/gene/cyp2c19/' where symbol=upper('cyp2c19');
update gene set url='https://cpicpgx.org/gene/cyp2c9/' where symbol=upper('cyp2c9');
