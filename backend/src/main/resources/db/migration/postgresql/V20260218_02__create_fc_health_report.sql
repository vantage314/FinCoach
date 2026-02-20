CREATE TABLE IF NOT EXISTS fc_health_report (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  report_date TIMESTAMP NOT NULL,
  risk_score INT NULL,
  health_score INT NULL,
  behavior_score INT NULL,
  metrics_json TEXT NULL,
  advice_json TEXT NULL,
  rule_version VARCHAR(32) NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fc_health_report_user_date ON fc_health_report(user_id, report_date);
