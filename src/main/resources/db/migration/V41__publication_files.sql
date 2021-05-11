alter table publication add fulltextfile text;

COMMENT ON COLUMN publication.fulltextfile IS 'publicly-accessible URL to full text of publication';

create table publication_supplement
(
    id integer primary key default nextval('cpic_id'),
    publicationid integer references publication(id) not null,
    description text,
    url text not null,
    version INTEGER DEFAULT 1,

    unique (publicationid, url)
);

CREATE TRIGGER version_publication_supplement
    BEFORE UPDATE ON publication_supplement
    FOR EACH ROW EXECUTE PROCEDURE increment_version();

COMMENT ON TABLE publication_supplement IS 'A table to hold optional supplemental files for publications';
COMMENT ON COLUMN publication_supplement.id IS 'A synthetic numerical ID, primary key';
COMMENT ON COLUMN publication_supplement.publicationid IS 'A foreign-key reference to the publication this is a supplement for';
COMMENT ON COLUMN publication_supplement.description IS 'An optional free text description of the supplemental file';
COMMENT ON COLUMN publication_supplement.url IS 'The publicly-accessible URL to the supplemental file';
COMMENT ON COLUMN publication_supplement.version IS 'The version number, iterates on modification';

grant select,insert,update,delete on cpic.publication_supplement to cpic_api;
grant select on cpic.publication_supplement to web_anon;

update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/NSAID/2020/32189324.pdf' where pmid='32189324';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703.pdf' where pmid='25974703';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447.pdf' where pmid='23486447';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/TCA/2016/27997040.pdf' where pmid='27997040';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/abacavir/2012/22378157.pdf' where pmid='22378157';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/abacavir/2014/24561393.pdf' where pmid='24561393';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/allopurinol/2013/23232549.pdf' where pmid='23232549';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/allopurinol/2015/26094938.pdf' where pmid='26094938';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/atazanavir/2015/26417955.pdf' where pmid='26417955';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/atomoxetine/2019/30801677.pdf' where pmid='30801677';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/carbamazepine/2013/23695185.pdf' where pmid='23695185';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/clopidogrel/2011/21716271.pdf' where pmid='21716271';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/clopidogrel/2013/23698643.pdf' where pmid='23698643';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192.pdf' where pmid='22205192';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010.pdf' where pmid='24458010';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/efavirenz/2019/31006110.pdf' where pmid='31006110';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/fluoropyrimidines/2013/23988873.pdf' where pmid='23988873';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/fluoropyrimidines/2017/29152729.pdf' where pmid='29152729';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/ivacaftor/2014/24598717.pdf' where pmid='24598717';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/ondansetron/2016/28002639.pdf' where pmid='28002639';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/pegintron/2013/24096968.pdf' where pmid='24096968';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164.pdf' where pmid='25099164';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/rasburicase/2014/24787449.pdf' where pmid='24787449';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/simvastatin/2012/22617227.pdf' where pmid='22617227';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/simvastatin/2014/24918167.pdf' where pmid='24918167';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146.pdf' where pmid='25801146';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/thiopurines/2011/21270794.pdf' where pmid='21270794';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/thiopurines/2013/23422873.pdf' where pmid='23422873';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/thiopurines/2018/30447069.pdf' where pmid='30447069';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/volatile_anesthetic_succinylcholine/2018/30499100.pdf' where pmid='30499100';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/voriconazole/2016/27981572.pdf' where pmid='27981572';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/warfarin/2011/21900891.pdf' where pmid='21900891';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/PPI/2020/32770672.pdf' where pmid='32770672';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/28198005.pdf' where pmid='28198005';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/opioids/2020/33387367.pdf' where pmid='33387367';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/phenytoin/2020/32779747.pdf' where pmid='32779747';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/tamoxifen/2017/29385237.pdf' where pmid='29385237';
update publication set fulltextfile='https://files.cpicpgx.org/data/guideline/publication/carbamazepine/2017/29392710.pdf' where pmid='29392710';

insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/NSAID/2020/32189324-supplement.pdf' from publication where pmid='32189324';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/PPI/2020/32770672-supplement.pdf' from publication where pmid='32770672';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703-supplement.pdf' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_2015_CYP2C19_SSRI_translation_table.xlsx' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_2015_CYP2D6_SSRI_translation_table.xlsx' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2C19_frequency_table.xlsx' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2C19_frequency_table_legend.pdf' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2D6_frequency_table_(R3).xlsx' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2D6_frequency_table_legend_(R3).pdf' from publication where pmid='25974703';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-CYP2D6_frequency_table_(R2).xlsx' from publication where pmid='23486447';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-CYP2D6_frequency_table_legend_(R2).pdf' from publication where pmid='23486447';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-supplement.pdf' from publication where pmid='23486447';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/TCA/2016/27997040-supplement.pdf' from publication where pmid='27997040';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/TCA/2016/Amitriptyline_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx' from publication where pmid='27997040';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/TCA/2016/Nortriptyline_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx' from publication where pmid='27997040';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/abacavir/2012/22378157-supplement.pdf' from publication where pmid='22378157';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/abacavir/2014/24561393-supplement.pdf' from publication where pmid='24561393';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/allopurinol/2013/23232549-supplement.pdf' from publication where pmid='23232549';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/allopurinol/2015/26094938-supplement.pdf' from publication where pmid='26094938';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/atazanavir/2015/26417955-UGT1A1_allele_frequency.xlsx' from publication where pmid='26417955';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/atazanavir/2015/26417955-supplement.pdf' from publication where pmid='26417955';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/atomoxetine/2019/30801677-supplement.pdf' from publication where pmid='30801677';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/carbamazepine/2013/23695185-supplement.pdf' from publication where pmid='23695185';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/carbamazepine/2017/29392710-supplement.pdf' from publication where pmid='29392710';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/clopidogrel/2011/21716271-supplement.pdf' from publication where pmid='21716271';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/clopidogrel/2013/23698643-supplement.pdf' from publication where pmid='23698643';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-CYP2D6-allele_frequency_table.xlsx' from publication where pmid='22205192';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-CYP2D6-allele_table_and_legend.pdf' from publication where pmid='22205192';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-supplement.pdf' from publication where pmid='22205192';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-CYP2D6_allele_frequency_table_(R2).xlsx' from publication where pmid='24458010';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-CYP2D6_allele_table_and_legend_(R2).pdf' from publication where pmid='24458010';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-supplement.pdf' from publication where pmid='24458010';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/efavirenz/2019/31006110-supplement.pdf' from publication where pmid='31006110';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/fluoropyrimidines/2013/23988873-supplement.pdf' from publication where pmid='23988873';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/fluoropyrimidines/2017/29152729-supplement.pdf' from publication where pmid='29152729';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/ivacaftor/2014/24598717-supplement.pdf' from publication where pmid='24598717';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/ondansetron/2016/28002639-supplement.pdf' from publication where pmid='28002639';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/ondansetron/2016/Ondansetron_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx' from publication where pmid='28002639';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/opioids/2020/33387367-supplement.pdf' from publication where pmid='33387367';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/pegintron/2013/24096968-supplement.pdf' from publication where pmid='24096968';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-Phenytoin_translation_table_final.xlsx' from publication where pmid='25099164';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-supplement.doc' from publication where pmid='25099164';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-supplement.pdf' from publication where pmid='25099164';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/phenytoin/2020/32779747-supplement.pdf' from publication where pmid='32779747';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/rasburicase/2014/24787449-supplement.pdf' from publication where pmid='24787449';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/simvastatin/2012/22617227-supplement.pdf' from publication where pmid='22617227';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/simvastatin/2014/24918167-supplement.pdf' from publication where pmid='24918167';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/simvastatin/2014/24918167.xlsx' from publication where pmid='24918167';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-CYP3A5_allele_frequency_table.xlsx' from publication where pmid='25801146';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-CYP3A5_translation_table.xlsx' from publication where pmid='25801146';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-supplement.pdf' from publication where pmid='25801146';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/tamoxifen/2017/29385237-supplement.pdf' from publication where pmid='29385237';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/thiopurines/2011/21270794-supplement.pdf' from publication where pmid='21270794';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/thiopurines/2013/23422873-supplement.pdf' from publication where pmid='23422873';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/thiopurines/2018/30447069-supplement.pdf' from publication where pmid='30447069';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/volatile_anesthetic_succinylcholine/2018/30499100-supplement.pdf' from publication where pmid='30499100';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/voriconazole/2016/27981572-supplement.pdf' from publication where pmid='27981572';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/voriconazole/2016/Voriconazole_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx' from publication where pmid='27981572';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/warfarin/2011/21900891-supplement.pdf' from publication where pmid='21900891';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/warfarin/2011/IWPC_dose_calculator.xls' from publication where pmid='21900891';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/28198005-supplement.pdf' from publication where pmid='28198005';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/CYP2C9_frequency_table.xlsx' from publication where pmid='28198005';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/CYP4F2_frequency_table.xlsx' from publication where pmid='28198005';
insert into publication_supplement(publicationid, url) select id, 'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/VKORC1_frequency_table.xlsx' from publication where pmid='28198005';

update publication_supplement set description='Supplement to publication' where array_length(regexp_match(url, 'supplement'),1)=1;
