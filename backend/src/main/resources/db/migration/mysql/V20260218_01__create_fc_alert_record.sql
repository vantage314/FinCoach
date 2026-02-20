CREATE TABLE IF NOT EXISTS fc_alert_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  rule_key VARCHAR(128) NOT NULL,
  severity VARCHAR(32) NOT NULL,
  payload_json TEXT NULL,
  message VARCHAR(255) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_fc_alert_record_status (status),
  KEY idx_fc_alert_record_user (user_id),
  KEY idx_fc_alert_record_created (created_at)
);