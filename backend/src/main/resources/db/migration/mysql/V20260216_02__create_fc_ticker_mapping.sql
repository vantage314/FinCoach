CREATE TABLE IF NOT EXISTS fc_ticker_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  keyword VARCHAR(128) NOT NULL,
  ticker VARCHAR(64) NOT NULL,
  market VARCHAR(16) NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticker_mapping_keyword ON fc_ticker_mapping(keyword);
CREATE INDEX idx_ticker_mapping_enabled_priority ON fc_ticker_mapping(enabled, priority);
