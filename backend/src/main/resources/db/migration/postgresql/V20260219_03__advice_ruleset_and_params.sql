CREATE TABLE IF NOT EXISTS fc_advice_rule_set (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  version INT NOT NULL,
  enabled SMALLINT NOT NULL DEFAULT 0,
  description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_fc_advice_rule_set_code_version ON fc_advice_rule_set(code, version);
CREATE INDEX idx_fc_advice_rule_set_code_enabled ON fc_advice_rule_set(code, enabled);
CREATE INDEX idx_fc_advice_rule_set_enabled ON fc_advice_rule_set(enabled);

CREATE TABLE IF NOT EXISTS fc_advice_rule_param (
  id BIGSERIAL PRIMARY KEY,
  rule_set_code VARCHAR(64) NOT NULL,
  param_key VARCHAR(64) NOT NULL,
  param_value VARCHAR(512) NOT NULL,
  value_type VARCHAR(16) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_fc_advice_rule_param ON fc_advice_rule_param(rule_set_code, param_key);
CREATE INDEX idx_fc_advice_rule_param_rule_set ON fc_advice_rule_param(rule_set_code);

INSERT INTO fc_advice_rule_set (code, version, enabled, description, created_at, updated_at)
VALUES ('DEFAULT', 1, 1, 'Default advice rules', NOW(), NOW())
ON CONFLICT (code, version) DO NOTHING;

INSERT INTO fc_advice_rule_param
  (rule_set_code, param_key, param_value, value_type, updated_at)
VALUES
  ('DEFAULT', 'REBALANCE_DRIFT_PCT', '0.05', 'DECIMAL', NOW()),
  ('DEFAULT', 'RISK_SCORE_LOW', '30', 'INT', NOW()),
  ('DEFAULT', 'RISK_SCORE_MID', '60', 'INT', NOW()),
  ('DEFAULT', 'RISK_SCORE_HIGH', '80', 'INT', NOW()),
  ('DEFAULT', 'BEHAVIOR_SCORE_WEIGHTS_JSON', '{"tradeFreq":0.4,"concentration":0.3,"cashDrag":0.3}', 'JSON', NOW()),
  ('DEFAULT', 'MAX_POSITIONS', '8', 'INT', NOW())
ON CONFLICT (rule_set_code, param_key) DO NOTHING;
