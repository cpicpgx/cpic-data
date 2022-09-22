UPDATE drug SET flowchart = 'https://files.cpicpgx.org/images/flow_chart/Primaquine_CDS_Flow_Chart.jpg' WHERE name='primaquine';

UPDATE drug
SET flowchart = 'https://files.cpicpgx.org/images/flow_chart/G6PD_High_Risk_Drug_CDS_Flow_Chart.jpg'
WHERE name in (
               'dapsone',
               'methylene blue',
               'pegloticase',
               'rasburicase',
               'tafenoquine',
               'toluidine blue'
    );

UPDATE drug SET flowchart = 'https://files.cpicpgx.org/images/flow_chart/G6PD_Medium_Risk_Drug_CDS_Flow_Chart.jpg' WHERE name='nitrofurantoin';

UPDATE drug
SET flowchart = 'https://files.cpicpgx.org/images/flow_chart/G6PD_Low-to-no_Risk_Drug_CDS_Flow_Chart.jpg'
WHERE name in (
               'aminosalicylic acid',
               'aspirin',
               'chloramphenicol',
               'chloroquine',
               'ciprofloxacin',
               'dimercaprol',
               'doxorubicin',
               'furazolidone',
               'glyburide',
               'hydroxychloroquine',
               'mafenide',
               'nalidixic acid',
               'norfloxacin',
               'ofloxacin',
               'phenazopyridine',
               'quinine',
               'sulfadiazine',
               'sulfadimidine',
               'sulfamethoxazole / trimethoprim',
               'sulfanilamide',
               'sulfasalazine',
               'sulfisoxazole',
               'tolbutamide',
               'vitamin c',
               'vitamin k'
    );
