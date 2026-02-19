CREATE TABLE IF NOT EXISTS fc_advice_rule_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  version INT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  description VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fc_advice_rule_set_code_version (code, version),
  INDEX idx_fc_advice_rule_set_code_enabled (code, enabled),
  INDEX idx_fc_advice_rule_set_enabled (enabled)
);

CREATE TABLE IF NOT EXISTS fc_advice_rule_param (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_set_code VARCHAR(64) NOT NULL,
  param_key VARCHAR(64) NOT NULL,
  param_value VARCHAR(512) NOT NULL,
  value_type VARCHAR(16) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fc_advice_rule_param (rule_set_code, param_key),
  INDEX idx_fc_advice_rule_param_rule_set (rule_set_code)
);

INSERT IGNORE INTO fc_advice_rule_set (code, version, enabled, description, created_at, updated_at)
VALUES ('DEFAULT', 1, 1, 'Default advice rules', NOW(), NOW());

INSERT IGNORE INTO fc_advice_rule_param
  (rule_set_code, param_key, param_value, value_type, updated_at)
VALUES
  ('DEFAULT', 'REBALANCE_DRIFT_PCT', '0.05', 'DECIMAL', NOW()),
  ('DEFAULT', 'RISK_SCORE_LOW', '30', 'INT', NOW()),
  ('DEFAULT', 'RISK_SCORE_MID', '60', 'INT', NOW()),
  ('DEFAULT', 'RISK_SCORE_HIGH', '80', 'INT', NOW()),
  ('DEFAULT', 'BEHAVIOR_SCORE_WEIGHTS_JSON', '{"tradeFreq":0.4,"concentration":0.3,"cashDrag":0.3}', 'JSON', NOW()),
  ('DEFAULT', 'MAX_POSITIONS', '8', 'INT', NOW());
