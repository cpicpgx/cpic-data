CREATE TABLE test_alerts
(
  id INTEGER PRIMARY KEY DEFAULT nextval('cpic_id'),
  cds_context TEXT NOT NULL,
  trigger_condition TEXT[],
  drugId VARCHAR(20) REFERENCES drug(drugId),
  reference_point TEXT,
  activity_score TEXT,
  alert_text TEXT[] NOT NULL
);

COMMENT ON TABLE test_alerts IS 'Example CDS test alert language';
COMMENT ON COLUMN test_alerts.id IS 'A synthetic numerical ID, primary key';
COMMENT ON COLUMN test_alerts.cds_context IS 'This should be either pre-test or post-text';
COMMENT ON COLUMN test_alerts.trigger_condition IS 'An array of one more more descriptions of trigger conditions';
COMMENT ON COLUMN test_alerts.drugId IS 'The ID of a drug this alert text is for';
COMMENT ON COLUMN test_alerts.reference_point IS 'A reference to a labeled part of the flow chart that accompanies this alert, optional';
COMMENT ON COLUMN test_alerts.activity_score IS 'A description of the activity score criteria, optional';
COMMENT ON COLUMN test_alerts.alert_text IS 'An array of one or more pieces of alert text';
