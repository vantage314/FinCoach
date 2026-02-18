CREATE TABLE IF NOT EXISTS fc_health_report (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  report_date DATETIME NOT NULL,
  risk_score INT NULL,
  health_score INT NULL,
  behavior_score INT NULL,
  metrics_json MEDIUMTEXT NULL,
  advice_json MEDIUMTEXT NULL,
  rule_version VARCHAR(32) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_fc_health_report_user_date (user_id, report_date)
);