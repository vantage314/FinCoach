CREATE TABLE IF NOT EXISTS fc_score_rule_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fc_score_rule_set_code_version (code, version),
  INDEX idx_fc_score_rule_set_enabled (enabled)
);

CREATE TABLE IF NOT EXISTS fc_score_rule_param (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_set_id BIGINT NOT NULL,
  param_key VARCHAR(64) NOT NULL,
  param_value VARCHAR(255) NOT NULL,
  value_type VARCHAR(16) NOT NULL,
  min_value VARCHAR(64) NULL,
  max_value VARCHAR(64) NULL,
  description VARCHAR(255) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fc_score_rule_param (rule_set_id, param_key),
  INDEX idx_fc_score_rule_param_rule_set (rule_set_id)
);

INSERT IGNORE INTO fc_score_rule_set (code, name, version, enabled, published_at, created_at, updated_at)
VALUES ('DEFAULT', 'Default Rule Set', 1, 1, NOW(), NOW(), NOW());

SET @rule_set_id = (SELECT id FROM fc_score_rule_set WHERE code = 'DEFAULT' AND version = 1 LIMIT 1);

INSERT IGNORE INTO fc_score_rule_param
  (rule_set_id, param_key, param_value, value_type, min_value, max_value, description, updated_at)
VALUES
  (@rule_set_id, 'W_SHARPE', '0.35', 'DECIMAL', '0', '1', 'Sharpe weight', NOW()),
  (@rule_set_id, 'W_MDD', '0.35', 'DECIMAL', '0', '1', 'Max drawdown weight', NOW()),
  (@rule_set_id, 'W_VOL', '0.15', 'DECIMAL', '0', '1', 'Volatility weight', NOW()),
  (@rule_set_id, 'W_DIVERSIFICATION', '0.15', 'DECIMAL', '0', '1', 'Diversification weight', NOW()),
  (@rule_set_id, 'MDD_BAD', '0.40', 'DECIMAL', '0', '1', 'Max drawdown bad threshold', NOW()),
  (@rule_set_id, 'MDD_OK', '0.20', 'DECIMAL', '0', '1', 'Max drawdown ok threshold', NOW()),
  (@rule_set_id, 'SHARPE_OK', '0.50', 'DECIMAL', '-5', '5', 'Sharpe ok threshold', NOW()),
  (@rule_set_id, 'SHARPE_GOOD', '1.00', 'DECIMAL', '-5', '5', 'Sharpe good threshold', NOW()),
  (@rule_set_id, 'CASH_FLOW_RATE_MIN', '0.10', 'DECIMAL', '0', '1', 'Min cash flow rate', NOW()),
  (@rule_set_id, 'DEBT_RATIO_MAX', '0.50', 'DECIMAL', '0', '1', 'Max debt ratio', NOW()),
  (@rule_set_id, 'EMERGENCY_MONTHS_MIN', '3', 'INT', '0', '24', 'Min emergency months', NOW()),
  (@rule_set_id, 'ASSET_LIABILITY_RATIO_GOOD', '2.00', 'DECIMAL', '0', '100', 'Asset/liability good ratio', NOW()),
  (@rule_set_id, 'LIQUIDITY_RATIO_GOOD', '0.20', 'DECIMAL', '0', '1', 'Liquidity ratio good threshold', NOW());
