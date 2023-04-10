do $$
    DECLARE
        guidelineName TEXT := 'CYP2D6, CYP2C19, CYP2B6, SLC6A4, HTR2A and Serotonin Reuptake Inhibitor Antidepressants';
        guidelineUrl  TEXT := 'https://cpicpgx.org/guidelines/cpic-guideline-for-ssri-and-snri-antidepressants/';
        ssriGuidelineId INT  := -1;
        pubId INT := -1;
    BEGIN
        -- update guideline info to supercede the previous simvastatin guideline
        update guideline set name=guidelineName, genes='{CYP2D6, CYP2C19, CYP2B6, SLC6A4, HTR2A}', url=guidelineUrl, pharmgkbId='{}'
        where name='CYP2D6, CYP2C19 and Selective Serotonin Reuptake Inhibitors' returning id into ssriGuidelineId;

        update publication set guidelineid=ssriGuidelineId, url='https://files.cpicpgx.org/data/guideline/publication/serotonin_reuptake_inhibitor_antidepressants/2023/37032427.pdf'
                           where pmid='37032427' returning id into pubId;
        insert into publication_supplement(publicationid, description, url)
        values (pubId, 'Supplement to publication', 'https://files.cpicpgx.org/data/guideline/publication/serotonin_reuptake_inhibitor_antidepressants/2023/37032427-supplement.pdf');

        -- update drugs
        update drug set guidelineid=(select id from guideline where name=guidelineName)
        where name in (
                       'citalopram','escitalopram','paroxetine','vortioxetine','fluvoxamine','sertraline','venlafaxine'
            );
        -- new flow charts
        update drug set flowChart='https://files.cpicpgx.org/images/flow_chart/Venlafaxine_CDS_Flow_Chart.jpg' where name='venlafaxine';
        update drug set flowChart='https://files.cpicpgx.org/images/flow_chart/Vortioxetine_CDS_Flow_Chart.jpg' where name='vortioxetine';
    END $$
