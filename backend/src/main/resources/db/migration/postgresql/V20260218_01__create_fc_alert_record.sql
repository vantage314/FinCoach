CREATE TABLE IF NOT EXISTS fc_alert_record (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NULL,
  rule_key VARCHAR(128) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  payload_json TEXT NULL,
  message VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fc_alert_record_status ON fc_alert_record(status);
CREATE INDEX idx_fc_alert_record_user ON fc_alert_record(user_id);
CREATE INDEX idx_fc_alert_record_created ON fc_alert_record(created_at);
