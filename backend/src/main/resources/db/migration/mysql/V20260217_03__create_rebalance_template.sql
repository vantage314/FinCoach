CREATE TABLE IF NOT EXISTS fc_rebalance_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  version INT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  template_json TEXT NOT NULL,
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_fc_rebalance_template_code_version (code, version),
  INDEX idx_fc_rebalance_template_enabled (enabled)
);

INSERT IGNORE INTO fc_rebalance_template (code, name, version, enabled, template_json, published_at, created_at, updated_at)
VALUES (
  'BALANCED',
  'Balanced Template',
  1,
  1,
  '{"CASH":0.15,"BOND":0.30,"STOCK":0.35,"GOLD":0.10,"ETF":0.05,"OTHER":0.05}',
  NOW(),
  NOW(),
  NOW()
);
