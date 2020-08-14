-- noinspection SqlWithoutWhere
update guideline set genes=(select array_agg(distinct genesymbol) from pair where guidelineid=id and cpiclevel ~ 'A');

update pair set usedforrecommendation=true where guidelineid is not null;
update pair set usedforrecommendation=false where genesymbol='CYP2C8' and drugid='RxNorm:5640';
update pair set usedforrecommendation=false where genesymbol='HLA-A' and drugid='RxNorm:32624';
update pair set usedforrecommendation=false where genesymbol='CYP2C8' and drugid='RxNorm:3355';
