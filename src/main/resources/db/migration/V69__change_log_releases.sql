alter table change_log add deployedrelease text;

comment on column change_log.deployedrelease is 'The release this change was deployed with';

update change_log set deployedrelease='v1.39.0' where date<='2024-03-28'::date;
update change_log set deployedrelease='v1.38.0' where date<='2024-02-15'::date;
update change_log set deployedrelease='v1.37.0' where date<='2024-01-12'::date;
update change_log set deployedrelease='v1.36.0' where date<='2023-12-21'::date;
update change_log set deployedrelease='v1.35.0' where date<='2023-12-18'::date;
update change_log set deployedrelease='v1.34.0' where date<='2023-12-15'::date;
update change_log set deployedrelease='v1.33.0' where date<='2023-10-24'::date;
update change_log set deployedrelease='v1.32.0' where date<='2023-10-02'::date;
update change_log set deployedrelease='v1.31.1' where date<='2023-09-26'::date;
update change_log set deployedrelease='v1.31.0' where date<='2023-09-01'::date;
update change_log set deployedrelease='v1.30.0' where date<='2023-08-07'::date;
update change_log set deployedrelease='v1.29.0' where date<='2023-08-01'::date;
update change_log set deployedrelease='v1.28.0' where date<='2023-07-13'::date;
update change_log set deployedrelease='v1.27.0' where date<='2023-07-12'::date;
update change_log set deployedrelease='v1.26.0' where date<='2023-06-05'::date;
update change_log set deployedrelease='v1.25.0' where date<='2023-04-12'::date;
update change_log set deployedrelease='v1.24.0' where date<='2023-03-23'::date;
update change_log set deployedrelease='v1.23.1' where date<='2023-03-09'::date;
update change_log set deployedrelease='v1.23.0' where date<='2023-02-27'::date;
update change_log set deployedrelease='v1.22.2' where date<='2023-02-03'::date;
update change_log set deployedrelease='v1.22.1' where date<='2023-01-31'::date;
update change_log set deployedrelease='v1.22.0' where date<='2023-01-26'::date;
update change_log set deployedrelease='v1.21.6' where date<='2022-12-21'::date;
update change_log set deployedrelease='v1.21.5' where date<='2022-12-20'::date;
update change_log set deployedrelease='v1.21.4' where date<='2022-12-20'::date;
update change_log set deployedrelease='v1.21.3' where date<='2022-10-31'::date;
update change_log set deployedrelease='v1.21.2' where date<='2022-10-24'::date;
update change_log set deployedrelease='v1.21.1' where date<='2022-10-20'::date;
update change_log set deployedrelease='v1.21.0' where date<='2022-10-04'::date;
update change_log set deployedrelease='v1.20.0' where date<='2022-09-14'::date;
update change_log set deployedrelease='v1.19.1' where date<='2022-08-25'::date;
update change_log set deployedrelease='v1.19.0' where date<='2022-08-15'::date;
update change_log set deployedrelease='v1.18.20' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.19' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.18' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.17' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.16' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.15' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.14' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.13' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.12' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.11' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.10' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.9' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.8' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.7' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.6' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.5' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.4' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.3' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.2' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.1' where date<='2022-06-14'::date;
update change_log set deployedrelease='v1.18.0' where date<='2022-06-10'::date;
update change_log set deployedrelease='v1.17.1' where date<='2022-04-07'::date;
update change_log set deployedrelease='v1.16' where date<='2022-03-23'::date;
update change_log set deployedrelease='v1.15.1' where date<='2022-03-11'::date;
update change_log set deployedrelease='v1.15' where date<='2022-03-09'::date;
update change_log set deployedrelease='v1.14' where date<='2022-02-18'::date;
update change_log set deployedrelease='v1.13' where date<='2022-02-16'::date;
update change_log set deployedrelease='v1.12' where date<='2021-12-07'::date;
update change_log set deployedrelease='v1.11' where date<='2021-12-02'::date;
update change_log set deployedrelease='v1.10' where date<='2021-10-22'::date;
update change_log set deployedrelease='v1.9' where date<='2021-09-09'::date;
update change_log set deployedrelease='v1.8' where date<='2021-08-18'::date;
update change_log set deployedrelease='v1.7' where date<='2021-07-09'::date;
update change_log set deployedrelease='v1.6' where date<='2021-06-13'::date;
update change_log set deployedrelease='v1.5' where date<='2021-05-12'::date;
update change_log set deployedrelease='v1.4' where date<='2021-05-05'::date;
update change_log set deployedrelease='v1.3' where date<='2021-04-15'::date;
update change_log set deployedrelease='v1.2' where date<='2021-03-24'::date;
update change_log set deployedrelease='v1.1' where date<='2021-01-07'::date;
update change_log set deployedrelease='v1.0' where date<='2020-11-04'::date;
update change_log set deployedrelease='v0.11' where date<='2020-10-28'::date;
update change_log set deployedrelease='v0.10' where date<='2020-09-30'::date;
update change_log set deployedrelease='v0.9' where date<='2020-09-15'::date;
update change_log set deployedrelease='v0.8.1' where date<='2020-08-16'::date;
update change_log set deployedrelease='v0.8' where date<='2020-08-14'::date;
update change_log set deployedrelease='v0.7' where date<='2020-07-04'::date;
update change_log set deployedrelease='v0.6' where date<='2020-05-17'::date;
update change_log set deployedrelease='v0.5' where date<='2019-05-03'::date;
update change_log set deployedrelease='v0.4' where date<='2019-04-12'::date;
update change_log set deployedrelease='v0.3.1' where date<='2019-01-14'::date;
update change_log set deployedrelease='v0.3' where date<='2019-01-14'::date;
update change_log set deployedrelease='v0.2' where date<='2018-12-11'::date;
update change_log set deployedrelease='v0.1' where date<='2018-10-09'::date;

DROP VIEW cpic.change_log_view;
CREATE VIEW cpic.change_log_view AS
with x as (
    select symbol as entityid, symbol as entityname from gene
    union all
    select drugid as entityid, name as entityname from drug
)
select
    l.date,
    l.type,
    x.entityname,
    l.note,
    l.deployedrelease
from
    change_log l
        left join x on (l.entityid=x.entityid);
COMMENT ON VIEW cpic.change_log_view IS 'This view of the change_log table add the entity name to make it more readable';
