create view file_status as
select a.*, history.latest_change
from file_artifact a
         left join (select h.fileid, max(h.changedate) as latest_change from file_artifact_history h group by h.fileid) history on (a.id=history.fileid)
order by type, filename;
