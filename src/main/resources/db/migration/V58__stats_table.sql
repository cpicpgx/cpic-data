create table cpic.statistic (
    createdon timestamp,
    stattype text,
    statvalue numeric not null,

    primary key (createdon, stattype)
);
comment on table cpic.statistic is 'A collection of numerical statistics with metadata about this database and other CPIC data';
