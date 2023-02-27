alter table cpic.recommendation add dosingInformation bool default false;
comment on column cpic.recommendation.dosingInformation is 'The guideline recommends a dose adjustment based on genetic variants or phenotypes (e.g. "poor metabolizers") as a primary course of action. However, the guideline may also describe extenuating circumstances where this action is not appropriate.';

alter table cpic.recommendation add alternateDrugAvailable bool default false;
comment on column cpic.recommendation.alternateDrugAvailable is 'The guideline recommends that an alternate drug be selected for patients based on genetic variants or phenotypes (e.g. "poor metabolizers") as a primary course of action. However, the guideline may also describe extenuating circumstances where this action is not appropriate.';

alter table cpic.recommendation add otherPrescribingGuidance bool default false;
comment on column cpic.recommendation.otherPrescribingGuidance is 'The guideline and other prescribing guidance not described as either dose adjustment or alternate drug.';
