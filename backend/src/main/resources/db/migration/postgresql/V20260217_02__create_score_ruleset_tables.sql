CREATE TABLE IF NOT EXISTS fc_score_rule_set (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  enabled SMALLINT NOT NULL DEFAULT 0,
  published_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_fc_score_rule_set_code_version ON fc_score_rule_set(code, version);
CREATE INDEX idx_fc_score_rule_set_enabled ON fc_score_rule_set(enabled);

CREATE TABLE IF NOT EXISTS fc_score_rule_param (
  id BIGSERIAL PRIMARY KEY,
  rule_set_id BIGINT NOT NULL,
  param_key VARCHAR(64) NOT NULL,
  param_value VARCHAR(255) NOT NULL,
  value_type VARCHAR(16) NOT NULL,
  min_value VARCHAR(64) NULL,
  max_value VARCHAR(64) NULL,
  description VARCHAR(255) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_fc_score_rule_param ON fc_score_rule_param(rule_set_id, param_key);
CREATE INDEX idx_fc_score_rule_param_rule_set ON fc_score_rule_param(rule_set_id);

INSERT INTO fc_score_rule_set (code, name, version, enabled, published_at, created_at, updated_at)
VALUES ('DEFAULT', 'Default Rule Set', 1, 1, NOW(), NOW(), NOW())
ON CONFLICT (code, version) DO NOTHING;

WITH rule_set AS (
  SELECT id FROM fc_score_rule_set WHERE code = 'DEFAULT' AND version = 1 LIMIT 1
),
params AS (
  SELECT * FROM (VALUES
    ('W_SHARPE', '0.35', 'DECIMAL', '0', '1', 'Sharpe weight'),
    ('W_MDD', '0.35', 'DECIMAL', '0', '1', 'Max drawdown weight'),
    ('W_VOL', '0.15', 'DECIMAL', '0', '1', 'Volatility weight'),
    ('W_DIVERSIFICATION', '0.15', 'DECIMAL', '0', '1', 'Diversification weight'),
    ('MDD_BAD', '0.40', 'DECIMAL', '0', '1', 'Max drawdown bad threshold'),
    ('MDD_OK', '0.20', 'DECIMAL', '0', '1', 'Max drawdown ok threshold'),
    ('SHARPE_OK', '0.50', 'DECIMAL', '-5', '5', 'Sharpe ok threshold'),
    ('SHARPE_GOOD', '1.00', 'DECIMAL', '-5', '5', 'Sharpe good threshold'),
    ('CASH_FLOW_RATE_MIN', '0.10', 'DECIMAL', '0', '1', 'Min cash flow rate'),
    ('DEBT_RATIO_MAX', '0.50', 'DECIMAL', '0', '1', 'Max debt ratio'),
    ('EMERGENCY_MONTHS_MIN', '3', 'INT', '0', '24', 'Min emergency months'),
    ('ASSET_LIABILITY_RATIO_GOOD', '2.00', 'DECIMAL', '0', '100', 'Asset/liability good ratio'),
    ('LIQUIDITY_RATIO_GOOD', '0.20', 'DECIMAL', '0', '1', 'Liquidity ratio good threshold')
  ) AS v(param_key, param_value, value_type, min_value, max_value, description)
)
INSERT INTO fc_score_rule_param
  (rule_set_id, param_key, param_value, value_type, min_value, max_value, description, updated_at)
SELECT rule_set.id,
       params.param_key,
       params.param_value,
       params.value_type,
       params.min_value,
       params.max_value,
       params.description,
       NOW()
FROM rule_set, params
ON CONFLICT (rule_set_id, param_key) DO NOTHING;
