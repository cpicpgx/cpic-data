-- the following "supplemental" files are outdated versions of files that are available elsewhere

insert into change_log(date, type, entityid, note)
select distinct
    date(now()) as date,
    'GUIDELINE' as type,
    p.guidelineid as entityid,
    'Removed redundant links to outdated supplemental files' as note
from
    publication_supplement s
        join publication p on p.id=s.publicationid
where
    s.url in (
              'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_2015_CYP2C19_SSRI_translation_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_2015_CYP2D6_SSRI_translation_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2C19_frequency_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2C19_frequency_table_legend.pdf',
              'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2D6_frequency_table_(R3).xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2D6_frequency_table_legend_(R3).pdf',
              'https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-CYP2D6_frequency_table_(R2).xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-CYP2D6_frequency_table_legend_(R2).pdf',
              'https://files.cpicpgx.org/data/guideline/publication/TCA/2016/Amitriptyline_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/TCA/2016/Nortriptyline_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/atazanavir/2015/26417955-UGT1A1_allele_frequency.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-CYP2D6-allele_frequency_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-CYP2D6-allele_table_and_legend.pdf',
              'https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-CYP2D6_allele_frequency_table_(R2).xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-CYP2D6_allele_table_and_legend_(R2).pdf',
              'https://files.cpicpgx.org/data/guideline/publication/ondansetron/2016/Ondansetron_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-Phenytoin_translation_table_final.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-supplement.doc',
              'https://files.cpicpgx.org/data/guideline/publication/simvastatin/2014/24918167.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-CYP3A5_allele_frequency_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-CYP3A5_translation_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/voriconazole/2016/Voriconazole_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/CYP2C9_frequency_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/CYP4F2_frequency_table.xlsx',
              'https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/VKORC1_frequency_table.xlsx'
        );


delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_2015_CYP2C19_SSRI_translation_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_2015_CYP2D6_SSRI_translation_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2C19_frequency_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2C19_frequency_table_legend.pdf';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2D6_frequency_table_(R3).xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/SSRI/2015/25974703_CYP2D6_frequency_table_legend_(R3).pdf';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-CYP2D6_frequency_table_(R2).xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/TCA/2013/23486447-CYP2D6_frequency_table_legend_(R2).pdf';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/TCA/2016/Amitriptyline_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/TCA/2016/Nortriptyline_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/atazanavir/2015/26417955-UGT1A1_allele_frequency.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-CYP2D6-allele_frequency_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/codeine/2012/22205192-CYP2D6-allele_table_and_legend.pdf';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-CYP2D6_allele_frequency_table_(R2).xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/codeine/2014/24458010-CYP2D6_allele_table_and_legend_(R2).pdf';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/ondansetron/2016/Ondansetron_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-Phenytoin_translation_table_final.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/phenytoin/2014/25099164-supplement.doc';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/simvastatin/2014/24918167.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-CYP3A5_allele_frequency_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/tacrolimus/2015/25801146-CYP3A5_translation_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/voriconazole/2016/Voriconazole_Pre_and_Post_Test_Alerts_and_Flow_Chart.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/CYP2C9_frequency_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/CYP4F2_frequency_table.xlsx';
delete from publication_supplement where url='https://files.cpicpgx.org/data/guideline/publication/warfarin/2017/VKORC1_frequency_table.xlsx';
